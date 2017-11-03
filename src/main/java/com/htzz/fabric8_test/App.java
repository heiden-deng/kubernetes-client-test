package com.htzz.fabric8_test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Hello world!
 *
 */
public class App 
{
	private static final Logger logger = LoggerFactory.getLogger(App.class);
    public static void main( String[] args )
    {
    	KubernetesClient kubernetesClient= connectK8s();
    	PodList pl = kubernetesClient.pods().list();
    	List<Pod> podList = pl.getItems();
    	for(Pod pod : podList) {
    		System.out.println("Pod Name=" + pod.getMetadata().getName() + "   status=" + pod.getStatus().toString());
    	}
    	Service sv = new ServiceBuilder()
    			.withNewMetadata().withName("nginx-ds-api").addToLabels("app", "nginx-ds-api").endMetadata()
    			.withNewSpec().withType("NodePort")
    			  .withSelector(new HashMap<String,String>(){{put("app", "nginx-ds-api");}})
    			  .withPorts(new ServicePortBuilder().withName("http").withPort(80).withNewTargetPort(80).build())
    			 .endSpec()
    			 .build(); 
    			  
    	List<Container> containers = new ArrayList<Container>();
    	Container container = new ContainerBuilder()
    			.withName("my-api-nginx")
    			.withImage("nginx:1.7.9")
    			.withPorts(new ContainerPortBuilder().withContainerPort(80).build())
    			.build();
    	containers.add(container);
    	DaemonSet ds = new DaemonSetBuilder()
    			.withNewMetadata().withName("nginx-ds-api").addToLabels("addonmanager.kubernetes.io/mode","Reconcile").endMetadata()
    			.withNewSpec()
    			 .withNewTemplate()
    			   .withNewMetadata().addToLabels("app", "nginx-ds-api").endMetadata()
    			   .withNewSpec().withContainers(containers)
    			   .endSpec()
    			 .endTemplate()
    			 .endSpec()
    			 .build();
			   
    	DaemonSet dsNew = kubernetesClient.extensions().daemonSets().inNamespace("default").create(ds);		  
    	log("create Daemonset",dsNew);
    	
    	Service svNew = kubernetesClient.services().inNamespace("default").create(sv);
    	log("create Service",svNew);
    	
    	PodList pl2 = kubernetesClient.pods().list();
    	List<Pod> podList2 = pl2.getItems();
    	for(Pod pod : podList2) {
    		System.out.println("Pod Name=" + pod.getMetadata().getName() + ",   status=" + pod.getStatus().toString());
    	}
    	
        System.out.println( "Hello World!" );
    }
    /**
     * 连接k8s master服务器
     * @return
     */
    public static KubernetesClient connectK8s(){
          String namespace = "default";
          String master = "http://172.17.70.11:8080/";
          KubernetesClient client=null;
          Config config = new ConfigBuilder().withMasterUrl(master)
                  .withTrustCerts(true)
                  .withNamespace(namespace).build();
          try {
                  client = new DefaultKubernetesClient(config);
              
          }catch (Exception e) {
                 logger.error(e.getMessage(), e);
          }
          return client;
    }
    private static void log(String action, Object obj) {
        logger.info("{}: {}", action, obj);
    }

    private static void log(String action) {
        logger.info(action);
    }
}
