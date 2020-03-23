

Intermediate step to rework

- Test paas expected health endpoint config which needs fix.

- Worked around start problems
    - need to review status of other tests in circle and schedule acceptance tests in concourse
    - need to review expected production behavior
    - need to add comments
    - need to document profiles and properties & include them in CICD arguments

- rename and comment the SecuredControllerSpringBootIntegrationTest

- DONE: need to review how paas-templates merges or replaces altogether application.yml: ops teams should rely on  safe defaults for operability (actuator)
   - currently paas-templates replaces the whole `BOOT-INF/classes/application.yml` 
    > zip -r  ${GENERATE_DIR}/${JAR_ARTEFACT_BASE_NAME}.jar BOOT-INF/classes/application.yml
   - alternatives
      - add a new file in the jar which spring boot would use to override the default application.yml
         - https://docs.spring.io/spring-boot/docs/2.2.5.RELEASE/reference/htmlsingle/#boot-features-external-config
            - Profile-specific application properties packaged inside your jar (application-{profile}.properties and YAML variants).
                - **Don't specify a profile, and keep `default` profile in `application-default.yml`**
                - Profile specific config should override  
      - look into java buildpack for ways to add external application.yml
         - https://github.com/cloudfoundry/java-buildpack/blob/master/docs/container-spring_boot.md
            > If the application uses Spring, Spring profiles can be specified by setting the SPRING_PROFILES_ACTIVE environment variable.                                                                                                                                                                                                                                                
   - test `application-default.yml` precedence                                                                                                                                                                                                                                           
      - include `application-default.yml` in the jar and test that this properly overrides `application.yml` config
         - an autowired property value in a springboot test
            - https://docs.spring.io/spring/docs/5.1.14.RELEASE/spring-framework-reference/core.html#expressions-beandef-annotation-based
            >  To specify a default value, you can place the @Value annotation on fields, methods, and method or constructor parameters.
            - https://www.baeldung.com/spring-value-annotation                                                                                                                                                                                             >  
         - Pb: profile "offline-test-without-scab" is already set
            - Q: can we specify multiple active profiles ?
            - A: yes, comma separated list of profiles https://docs.spring.io/spring-boot/docs/2.2.5.RELEASE/reference/htmlsingle/#boot-features-profiles     

--------------

Auth
* test
   * unit test which assert the spring security config ?
   * spring boot integration test that launch in circle ci, to verify the springboot config, and iterate in TDD over it. No need to test the real packaging of osb-cmdb
      * desired beans to launch: 
         * scosb controllers
         * security config
         * noop workflows ?
      * beans to exclude: 
         * scab osb impls
         * dynamic catalog
      * how to exclude them ?
         * explicit config class instead of springboot autoconfig ?
         * profile
            * any link to gradle properties ?
               * how to specify profile in test execution ?
                  * ` -Dspring.profiles.active=production` see https://docs.spring.io/spring-boot/docs/2.2.5.RELEASE/reference/htmlsingle/#howto-set-active-spring-profiles
                  
                  > 2020-03-20 17:24:59.230  INFO 23732 --- [           main] curedControllerSpringBootIntegrationTest : No active profile set, falling back to default profiles: default

         * start errors with profile
                  
                   > Parameter 0 of method dynamicCatalogService in org.springframework.cloud.appbroker.autoconfigure.DynamicCatalogServiceAutoConfiguration required a bean of type 'org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties' that could not be found.                                                                                                                                                                                                                             
                   * application.yml is enabling this by default
                      * turn off in application.yml
                      * override in test
                         > @TestPropertySource(properties = {
                         >  	"spring.cloud.appbroker.services[0].service-name=example",
                         >  	"spring.cloud.appbroker.services[0].plan-name=standard",
                         >  	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
                         >  	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
                         >  
                         >  	"spring.credhub.url=http://localhost:8888"
                         >  })
                                                                                                                                                                > Parameter 0 of method createServiceKeyWorkflow in com.orange.oss.osbcmdb.OsbCmdbApplication required a bean of type 'org.springframework.cloud.appbroker.deployer.BrokeredServices' that could not be found.
                  * Expected: 
                      * In prod, fail fast if config is missing
                      * In auth test: start without bean
                      * In acceptance test: fails is config missing
                  * @ConditionalOnBean: i.e. accept starting production app without service key workflow
                  * Exclude on specific profile (test) 
                                                        
         * class
         * property
         
                   >  Error creating bean with name 'beanCatalogService' defined in class path resource [org/springframework/cloud/servicebroker/autoconfigure/web/ServiceBrokerAutoConfiguration.class]
                   
                    "spring.cloud.openservicebroker.catalog.services[0]", name = "id"

                    >        @Configuration
                    >        @ConditionalOnMissingBean({Catalog.class, CatalogService.class})
                    >        @EnableConfigurationProperties(ServiceBrokerProperties.class)
                    >        @ConditionalOnProperty(prefix = "spring.cloud.openservicebroker.catalog.services[0]", name = "id")
                    >        protected static class CatalogPropertiesMinimalConfiguration {
                    >    
                    >            private final ServiceBrokerProperties serviceBrokerProperties;
                    >    
                    >            public CatalogPropertiesMinimalConfiguration(ServiceBrokerProperties serviceBrokerProperties) {
                    >                this.serviceBrokerProperties = serviceBrokerProperties;
                    >            }
                    >    
                    >            @Bean
                    >            public Catalog catalog() {
                    >                return this.serviceBrokerProperties.getCatalog().toModel();
                    >            }
                    >        }

