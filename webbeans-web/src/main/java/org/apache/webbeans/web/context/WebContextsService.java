/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.web.context;

import org.apache.webbeans.annotation.DestroyedLiteral;
import org.apache.webbeans.annotation.InitializedLiteral;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.AbstractContextsService;
import org.apache.webbeans.context.ApplicationContext;
import org.apache.webbeans.context.ConversationContext;
import org.apache.webbeans.context.DependentContext;
import org.apache.webbeans.context.RequestContext;
import org.apache.webbeans.context.SessionContext;
import org.apache.webbeans.context.SingletonContext;
import org.apache.webbeans.conversation.ConversationImpl;
import org.apache.webbeans.conversation.ConversationManager;
import org.apache.webbeans.el.ELContextStore;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.web.intercept.RequestScopedBeanInterceptorHandler;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextException;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Web container {@link org.apache.webbeans.spi.ContextsService}
 * implementation.
 */
public class WebContextsService extends AbstractContextsService
{
    /**Logger instance*/
    private static final Logger logger = WebBeansLoggerFacade.getLogger(WebContextsService.class);

    private static final String OWB_SESSION_CONTEXT_ATTRIBUTE = "OPENWEBBEANS_SESSION_CONTEXT";

    /**Current request context*/
    private static ThreadLocal<RequestContext> requestContexts = null;

    /**Current session context*/
    private static ThreadLocal<SessionContext> sessionContexts = null;

    /**
     * A single applicationContext
     */
    private ApplicationContext sharedApplicationContext ;

    private SingletonContext sharedSingletonContext;

    /**Current conversation context*/
    private static ThreadLocal<ConversationContext> conversationContexts = null;
    
    /**Current dependent context*/
    private static DependentContext dependentContext;

    /**Conversation context manager*/
    private final ConversationManager conversationManager;

    private boolean supportsConversation = false;
    
    private WebBeansContext webBeansContext;

    /**Initialize thread locals*/
    static
    {
        requestContexts = new ThreadLocal<RequestContext>();
        sessionContexts = new ThreadLocal<SessionContext>();
        conversationContexts = new ThreadLocal<ConversationContext>();

        //Dependent context is always active
        dependentContext = new DependentContext();
        dependentContext.setActive(true);
    }

    /**
     * Removes the ThreadLocals from the ThreadMap to prevent memory leaks.
     */
    public static void removeThreadLocals()
    {
        requestContexts.remove();
        sessionContexts.remove();
        conversationContexts.remove();
        RequestScopedBeanInterceptorHandler.removeThreadLocals();
    }
    
    /**
     * Creates a new instance.
     */
    public WebContextsService(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
        supportsConversation =  webBeansContext.getOpenWebBeansConfiguration().supportsConversation();
        conversationManager = webBeansContext.getConversationManager();

        sharedApplicationContext = new ApplicationContext();
        sharedApplicationContext.setActive(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Object initializeObject)
    {        
        //Start application context
        startContext(ApplicationScoped.class, initializeObject);
        
        //Start signelton context
        startContext(Singleton.class, initializeObject);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy(Object destroyObject)
    {
        //Destroy application context
        endContext(ApplicationScoped.class, destroyObject);

        // we also need to destroy the shared ApplicationContext
        sharedApplicationContext.destroy();
        
        //Destroy singleton context
        endContext(Singleton.class, destroyObject);
        if (sharedSingletonContext != null)
        {
            sharedSingletonContext.destroy();
        }

        //Thread local values to null
        requestContexts.set(null);
        sessionContexts.set(null);
        conversationContexts.set(null);

        //Remove thread locals
        //for preventing memory leaks
        requestContexts.remove();
        sessionContexts.remove();
        conversationContexts.remove();
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void endContext(Class<? extends Annotation> scopeType, Object endParameters)
    {        
        if(scopeType.equals(RequestScoped.class))
        {
            destroyRequestContext((ServletRequestEvent)endParameters);
        }
        else if(scopeType.equals(SessionScoped.class))
        {
            destroySessionContext((HttpSession)endParameters);
        }
        else if(scopeType.equals(ApplicationScoped.class))
        {
            destroyApplicationContext((ServletContext)endParameters);
        }
        else if(supportsConversation && scopeType.equals(ConversationScoped.class))
        {
            if (endParameters != null && endParameters instanceof HttpSession)
            {
                destoryAllConversationsForSession((HttpSession) endParameters);
            }

            destroyConversationContext();
        }
        else if(scopeType.equals(Dependent.class))
        {
            //Do nothing
        }
        else if (scopeType.equals(Singleton.class))
        {
            destroySingletonContext((ServletContext)endParameters);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context getCurrentContext(Class<? extends Annotation> scopeType)
    {
        if(scopeType.equals(RequestScoped.class))
        {
            return getRequestContext();
        }
        else if(scopeType.equals(SessionScoped.class))
        {
            return getSessionContext();
        }
        else if(scopeType.equals(ApplicationScoped.class))
        {
            return sharedApplicationContext;
        }
        else if(supportsConversation && scopeType.equals(ConversationScoped.class))
        {
            return getConversationContext();
        }
        else if(scopeType.equals(Dependent.class))
        {
            return dependentContext;
        }
        else if (scopeType.equals(Singleton.class))
        {
            return sharedSingletonContext;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startContext(Class<? extends Annotation> scopeType, Object startParameter) throws ContextException
    {
        if (scopeType.equals(RequestScoped.class))
        {
            initRequestContext((ServletRequestEvent)startParameter);
        }
        else if (scopeType.equals(SessionScoped.class))
        {
            initSessionContext((HttpSession)startParameter);
        }
        else if (scopeType.equals(ApplicationScoped.class))
        {
            initApplicationContext((ServletContext)startParameter);
        }
        else if (supportsConversation && scopeType.equals(ConversationScoped.class))
        {
            initConversationContext(startParameter);
        }
        else if (scopeType.equals(Dependent.class))
        {
            //Do nothing
        }
        else if (scopeType.equals(Singleton.class))
        {
            initSingletonContext((ServletContext)startParameter);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsContext(Class<? extends Annotation> scopeType)
    {
        if (scopeType.equals(RequestScoped.class) ||
            scopeType.equals(SessionScoped.class) ||
            scopeType.equals(ApplicationScoped.class) ||
            scopeType.equals(Dependent.class) ||
            scopeType.equals(Singleton.class) ||
            (scopeType.equals(ConversationScoped.class) && supportsConversation))
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * Initialize requext context with the given request object.
     * @param event http servlet request event
     */
    private void initRequestContext(ServletRequestEvent event)
    {
        
        RequestContext rq = new ServletRequestContext();
        rq.setActive(true);

        requestContexts.set(rq);// set thread local

        if(event != null)
        {
            HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
            ((ServletRequestContext)rq).setServletRequest(request);
            
            if (request != null)
            {
                //Re-initialize thread local for session
                HttpSession session = request.getSession(false);
                
                if(session != null)
                {
                    initSessionContext(session);    
                }

                initConversationContext(request);

                webBeansContext.getBeanManagerImpl().fireEvent(request, InitializedLiteral.INSTANCE_REQUEST_SCOPED);
            }
        }
        else
        {
            webBeansContext.getBeanManagerImpl().fireEvent(new Object(), InitializedLiteral.INSTANCE_REQUEST_SCOPED);
        }
    }
    
    /**
     * Destroys the request context and all of its components. 
     * @param requestEvent http servlet request object
     */
    private void destroyRequestContext(ServletRequestEvent requestEvent)
    {
        // cleanup open conversations first
        if (supportsConversation)
        {
            cleanupConversations();
        }


        //Get context
        RequestContext context = getRequestContext();

        //Destroy context
        if (context != null)
        {
            context.destroy();
        }
        
        // clean up the EL caches after each request
        ELContextStore elStore = ELContextStore.getInstance(false);
        if (elStore != null)
        {
            elStore.destroyELContextStore();
        }

        Object payload = requestEvent != null && requestEvent.getServletRequest() != null ? requestEvent.getServletRequest() : new Object();
        webBeansContext.getBeanManagerImpl().fireEvent(payload, DestroyedLiteral.INSTANCE_REQUEST_SCOPED);

        //Clear thread locals
        conversationContexts.set(null);
        conversationContexts.remove();
        sessionContexts.set(null);
        sessionContexts.remove();
        requestContexts.set(null);
        requestContexts.remove();

        RequestScopedBeanInterceptorHandler.removeThreadLocals();
    }

    private void cleanupConversations()
    {
        ConversationContext conversationContext = getConversationContext();

        if (conversationContext == null)
        {
            return;
        }

        Conversation conversation = conversationManager.getConversationBeanReference();

        if (conversation == null)
        {
            return;
        }

        if (conversation.isTransient())
        {
            if (logger.isLoggable(Level.FINE))
            {
                logger.log(Level.FINE, "Destroying the transient conversation context with cid : [{0}]", conversation.getId());
            }
            destroyConversationContext();
            conversationManager.removeConversation(conversation); // in case end() was called
        }
        else
        {
            //Conversation must be used by one thread at a time
            ConversationImpl owbConversation = (ConversationImpl)conversation;
            owbConversation.updateTimeOut();
            //Other threads can now access propogated conversation.
            owbConversation.iDontUseItAnymore();
        }
    }

    /**
     * Creates the session context at the session start.
     * Or assign a
     * @param session http session object
     */
    private void initSessionContext(HttpSession session)
    {
        SessionContext currentSessionContext;

        if (session == null)
        {
            // no session -> create a dummy SessionContext
            // this is handy if you create asynchronous tasks or
            // batches which use a 'admin' user.
            currentSessionContext = new SessionContext();
            webBeansContext.getBeanManagerImpl().fireEvent(new Object(), InitializedLiteral.INSTANCE_SESSION_SCOPED);
        }
        else
        {
            // we need to get it latest here to make sure we work on the same instance
            currentSessionContext = (SessionContext) session.getAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE);

            if (currentSessionContext == null)
            {
                // no current context, so lets create a new one
                synchronized (OWB_SESSION_CONTEXT_ATTRIBUTE)
                {
                    currentSessionContext = (SessionContext) session.getAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE);
                    if (currentSessionContext == null)
                    {
                        currentSessionContext = new SessionContext();
                        //Activate
                        currentSessionContext.setActive(true);
                        webBeansContext.getBeanManagerImpl().fireEvent(session, InitializedLiteral.INSTANCE_SESSION_SCOPED);
                    }
                }
            }

            // we do that in any case.
            // This is needed to trigger delta-replication on most servers
            session.setAttribute(OWB_SESSION_CONTEXT_ATTRIBUTE, currentSessionContext);
        }


        //Set thread local
        sessionContexts.set(currentSessionContext);
    }

    /**
     * Destroys the session context and all of its components at the end of the
     * session. 
     * @param session http session object. Can be {@code null} for non-http SessionContexts. Such a context only lives for one thread.
     */
    private void destroySessionContext(HttpSession session)
    {
        //Get current session context from ThreadLocal
        SessionContext context = sessionContexts.get();

        if (session != null)
        {
            if (context == null)
            {
                initSessionContext(session);
                context = sessionContexts.get();
            }
        }

        //Destroy context
        if (context != null)
        {
            context.destroy();
            webBeansContext.getBeanManagerImpl().fireEvent(session, DestroyedLiteral.INSTANCE_SESSION_SCOPED);
        }

        //Clear thread locals
        sessionContexts.set(null);
        sessionContexts.remove();

    }

    /**
     * Creates the application context at the application startup 
     * @param servletContext servlet context object
     */
    private void initApplicationContext(ServletContext servletContext)
    {
        if (sharedApplicationContext != null)
        {
            return;
        }

        ApplicationContext newApplicationContext = new ApplicationContext();
        newApplicationContext.setActive(true);

        if (sharedApplicationContext == null)
        {
            sharedApplicationContext = newApplicationContext;
            webBeansContext.getBeanManagerImpl().fireEvent(servletContext != null ? servletContext : new Object(), InitializedLiteral.INSTANCE_APPLICATION_SCOPED);
        }
    }

    /**
     * Destroys the application context and all of its components at the end of
     * the application. 
     * @param servletContext servlet context object
     */
    private void destroyApplicationContext(ServletContext servletContext)
    {
        //look for thread local
        //this can be set by initRequestContext
        ApplicationContext context = sharedApplicationContext;
        sharedSingletonContext = null;

        //Destroy context
        if(context != null)
        {
            context.destroy();
        }

        // this is needed to get rid of ApplicationScoped beans which are cached inside the proxies...
        webBeansContext.getBeanManagerImpl().clearCacheProxies();

        webBeansContext.getBeanManagerImpl().fireEvent(servletContext != null ? servletContext : new Object(), DestroyedLiteral.INSTANCE_APPLICATION_SCOPED);
    }
    
    /**
     * Initialize singleton context.
     * @param servletContext servlet context
     */
    private void initSingletonContext(ServletContext servletContext)
    {
        if (sharedSingletonContext != null)
        {
            return;
        }

        SingletonContext newSingletonContext = new SingletonContext();
        newSingletonContext.setActive(true);

        if (sharedSingletonContext == null)
        {
            sharedSingletonContext = newSingletonContext;
        }

        webBeansContext.getBeanManagerImpl().fireEvent(servletContext != null ? servletContext : new Object(), InitializedLiteral.INSTANCE_SINGLETON_SCOPED);
    }
    
    /**
     * Destroy singleton context.
     * @param servletContext servlet context
     */
    private void destroySingletonContext(ServletContext servletContext)
    {
        SingletonContext context = sharedSingletonContext;

        if (context != null)
        {
            context.destroy();
        }

        webBeansContext.getBeanManagerImpl().fireEvent(servletContext != null ? servletContext : new Object(), DestroyedLiteral.INSTANCE_SINGLETON_SCOPED);
    }

    /**
     * Initialize conversation context.
     * @param startObject either a ServletRequest or a ConversationContext
     */
    private void initConversationContext(Object startObject)
    {

        if (startObject != null && startObject instanceof ConversationContext)
        {
            ConversationContext context = (ConversationContext) startObject;
            context.setActive(true);
            conversationContexts.set(context);
        }
        else
        {
            if(conversationContexts.get() == null)
            {
                ConversationContext newContext = new ConversationContext();
                webBeansContext.getBeanManagerImpl().fireEvent(new Object(), InitializedLiteral.INSTANCE_CONVERSATION_SCOPED);

                newContext.setActive(true);
                
                conversationContexts.set(newContext);
            }
            else
            {
                conversationContexts.get().setActive(true);
            }
        }
    }

    /**
     * Destroy conversation context.
     */
    private void destroyConversationContext()
    {
        ConversationContext context = getConversationContext();

        if (context != null)
        {
            context.destroy();
            webBeansContext.getBeanManagerImpl().fireEvent(new Object(), DestroyedLiteral.INSTANCE_SINGLETON_SCOPED);
        }

        conversationContexts.set(null);
        conversationContexts.remove();
    }

    /**
     * Workaround for OWB-841
     *
     * @param session The current {@link HttpSession}
     */
    private void destoryAllConversationsForSession(HttpSession session)
    {
        Map<Conversation, ConversationContext> conversations =
                conversationManager.getAndRemoveConversationMapWithSessionId(session.getId());

        for (Entry<Conversation, ConversationContext> entry : conversations.entrySet())
        {
            conversationContexts.set(entry.getValue());

            entry.getValue().destroy();
            
            conversationContexts.set(null);
            conversationContexts.remove();
        }
    }
    
    /**
     * Get current request ctx.
     * @return request context
     */
    private  RequestContext getRequestContext()
    {
        return requestContexts.get();
    }

    /**
     * Get current session ctx.
     * @return session context
     */
    private  SessionContext getSessionContext()
    {
        SessionContext context = sessionContexts.get();
        if (null == context)
        {
            lazyStartSessionContext();
            context = sessionContexts.get();
        }

        return context;
    }

    /**
     * Get current conversation ctx.
     * @return conversation context
     */
    private  ConversationContext getConversationContext()
    {
        return conversationContexts.get();
    }

    /**
     * Try to lazily start the sessionContext
     */
    private void lazyStartSessionContext()
    {

        if (logger.isLoggable(Level.FINE))
        {
            logger.log(Level.FINE, ">lazyStartSessionContext");
        }

        Context context = getCurrentContext(RequestScoped.class);
        if (context instanceof ServletRequestContext)
        {
            ServletRequestContext requestContext = (ServletRequestContext) context;
            HttpServletRequest servletRequest = requestContext.getServletRequest();
            if (null != servletRequest)
            { // this could be null if there is no active request context
                try
                {
                    HttpSession currentSession = servletRequest.getSession(true);
                    initSessionContext(currentSession);

                    if (logger.isLoggable(Level.FINE))
                    {
                        logger.log(Level.FINE, "Lazy SESSION context initialization SUCCESS");
                    }
                }
                catch (Exception e)
                {
                    logger.log(Level.SEVERE, WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0013, e));
                }

            }
            else
            {
                logger.log(Level.WARNING, "Could NOT lazily initialize session context because NO active request context");
            }
        }
        else
        {
            logger.log(Level.WARNING, "Could NOT lazily initialize session context because of "+context+" RequestContext");
        }
    }


    /**
     * This might be needed when you aim to start a new thread in a WebApp.
     * @param scopeType
     */
    @Override
    public void activateContext(Class<? extends Annotation> scopeType)
    {
        if (scopeType.equals(SessionScoped.class))
        {
            // getSessionContext() implicitely creates and binds the SessionContext
            // to the current Thread if it doesn't yet exist.
            getSessionContext().setActive(true);
        }
        else
        {
            super.activateContext(scopeType);
        }
    }

}
