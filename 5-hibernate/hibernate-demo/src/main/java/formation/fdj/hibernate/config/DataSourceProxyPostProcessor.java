package formation.fdj.hibernate.config;

import java.lang.reflect.Method;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

public class DataSourceProxyPostProcessor implements BeanPostProcessor {
    // Méthode appelée avant l'initialisation d'un bean
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // Vérifie si le bean est une instance de DataSource
        if (bean instanceof DataSource) {
            // Crée une factory de proxy pour le bean
            ProxyFactory proxy = new org.springframework.aop.framework.ProxyFactory(bean);
            // Ajoute un advice pour intercepter les appels de méthodes sur ce bean
            proxy.addAdvice(new ProxyDataSourceInterceptor((DataSource) bean));
        }
        return bean; // Retourne le bean (proxy ou original)
    }

    // Méthode appelée après l'initialisation d'un bean
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean; // Retourne le bean sans modification
    }

    // Classe interne pour intercepter les méthodes du DataSource
    private static class ProxyDataSourceInterceptor implements MethodInterceptor {

        private final DataSource dataSource;

        // Constructeur qui crée un proxy DataSource avec des options de journalisation
        public ProxyDataSourceInterceptor(final DataSource dataSource) {
            super();
            this.dataSource = ProxyDataSourceBuilder.create(dataSource)
                    .name("DATA_SOURCE_PROXY") // Nom du proxy
                    .logQueryBySlf4j(SLF4JLogLevel.INFO) // Niveau de journalisation SLF4J
                    .multiline() // Affichage des requêtes sur plusieurs lignes
                    .countQuery() // Comptage des requêtes
                    .build();
        }

        // Méthode appelée pour chaque invocation de méthode sur le proxy
        @Override
        public Object invoke(final MethodInvocation invocation) throws Throwable {
            // Recherche la méthode correspondante dans le proxy DataSource
            final Method proxyMethod = ReflectionUtils.findMethod(this.dataSource.getClass(), invocation.getMethod().getName());

            if (proxyMethod != null) {
                // Si la méthode est trouvée, l'invoque avec les arguments d'origine
                return proxyMethod.invoke(this.dataSource, invocation.getArguments());
            }

            // Si la méthode n'est pas trouvée, procède avec l'invocation d'origine
            return invocation.proceed();
        }
    }
}
