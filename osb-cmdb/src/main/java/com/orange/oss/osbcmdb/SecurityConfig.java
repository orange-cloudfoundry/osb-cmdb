package com.orange.oss.osbcmdb;

import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 *
 * See https://docs.spring.io/spring-security/site/docs/5.3.0.RELEASE/reference/html5/#jc-httpsecurity DSL
 * documentation
 * In the future with spring security 5.2, consider using Lambda dsl, see https://spring.io/blog/2019/11/21/spring-security-lambda-dsl
 * <p>
 * SpringBoot provides a default basic auth configuration, see https://docs.spring.io/spring-boot/docs/2.2.5.RELEASE/reference/htmlsingle/#boot-features-security
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	private final Logger log = Loggers.getLogger(SecurityConfig.class);

	public static final String OSBCMDB_ADMIN_USER_PROP_NAME = "osbcmdb.admin.user";
	public static final String OSBCMDB_ADMIN_PASSWORD_PROP_NAME = "osbcmdb.admin.password";
	public static final String SPRINGBOOT_SECURITY_USER_PROP_NAME = "spring.security.user.name";
	public static final String SPRINGBOOT_SECURITY_PASSWORD_PROP_NAME = "spring.security.user.password";

	/**
	 * Password storage format prefix. In our inmemory password storage, when don't yet encrypt passwords
	 * See {@link org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration} for spring boot default behavior which does the same.
	 */
	private static final String NOOP_PASSWORD_PREFIX = "{noop}";

	//Note: could be moving this into a properties bean, referenced using @EnableConfigurationProperties
	@Value("${" + SPRINGBOOT_SECURITY_USER_PROP_NAME + "}")
	private String osbUser;

	@Value("${" + SPRINGBOOT_SECURITY_PASSWORD_PROP_NAME + "}")
	private String osbPassword;

	@Value("${" + OSBCMDB_ADMIN_USER_PROP_NAME + "}")
	private String adminUser;

	@Value("${" + OSBCMDB_ADMIN_PASSWORD_PROP_NAME + "}")
	private String adminPassword;


	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			// See https://docs.spring.io/spring-security/site/docs/5.3.0.RELEASE/reference/html5/#csrf-when
			//   Our recommendation is to use CSRF protection for any request that could be processed by a browser
			//   by normal users. If you are only creating a service that is used by non-browser clients,
			//   you will likely want to disable CSRF protection.
			.csrf().disable()

			.authorizeRequests()
			.antMatchers("/v2/**").authenticated() //OSB API needs be authenticated (typically with user osb)
			//Actuator config
			.requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
			.requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN") // actuactor sensitive end points need
			// admin role (usually admin user)
			.and()
			.httpBasic();
	}


	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> inMemoryAuthentication = auth
			.inMemoryAuthentication();
		configureAdminUser(inMemoryAuthentication);
		configureOsbUser(inMemoryAuthentication);
	}

	private void configureAdminUser(
		InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> inMemoryAuthentication) {
		if (adminUser != null && adminPassword != null) {
			inMemoryAuthentication.withUser(adminUser).password(NOOP_PASSWORD_PREFIX + adminPassword).roles(
				"ADMIN");
		}
		else {
			log.warn("No admin user configured with  property: {}={} and {}={}, actuator access will not be possible." +
					" Please update environment variables or application-default.yml to define them",
				OSBCMDB_ADMIN_USER_PROP_NAME, adminUser, OSBCMDB_ADMIN_PASSWORD_PROP_NAME, adminPassword);
		}
	}
	private void configureOsbUser(
		InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> inMemoryAuthentication) {
		if (osbUser != null && osbPassword != null) {
			inMemoryAuthentication.withUser(osbUser).password(NOOP_PASSWORD_PREFIX + osbPassword).roles(
				"OSB_USER");
		}
		else {
			log.warn("No admin user configured with  property: {}={} and {}={}, actuator access will not be possible." +
					" Please update environment variables or application-default.yml to define them",
				SPRINGBOOT_SECURITY_USER_PROP_NAME, osbUser, SPRINGBOOT_SECURITY_PASSWORD_PROP_NAME, osbPassword);
		}
	}

}
