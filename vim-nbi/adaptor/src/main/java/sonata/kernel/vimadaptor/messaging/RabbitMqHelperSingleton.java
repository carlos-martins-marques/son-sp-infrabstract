/*
 * Copyright (c) 2015 SONATA-NFV, UCL, NOKIA, THALES, NCSR Demokritos ALL RIGHTS RESERVED.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Neither the name of the SONATA-NFV, UCL, NOKIA, NCSR Demokritos nor the names of its contributors
 * may be used to endorse or promote products derived from this software without specific prior
 * written permission.
 * 
 * This work has been performed in the framework of the SONATA project, funded by the European
 * Commission under Grant number 671517 through the Horizon 2020 and 5G-PPP programmes. The authors
 * would like to acknowledge the contributions of their colleagues of the SONATA partner consortium
 * (www.sonata-nfv.eu).
 *
 * @author Dario Valocchi (Ph.D.), UCL
 * @author Carlos Marques (ALB)
 */
package sonata.kernel.vimadaptor.messaging;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class RabbitMqHelperSingleton {

  private static final String configFilePath = "/etc/son-mano/broker.config";
  private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(RabbitMqHelperSingleton.class);
  private static RabbitMqHelperSingleton myInstance = null;
  public static RabbitMqHelperSingleton getInstance() {
    if (myInstance == null) {
      myInstance = new RabbitMqHelperSingleton();
    }
    return myInstance;
  }

  private Channel northChannel;
  private Channel southChannel;

  private Connection northConnection;
  private Connection southConnection;

  private String northExchangeName;
  private String southExchangeName;

  private String northQueueName;
  private String southQueueName;

  private RabbitMqHelperSingleton() {
    Properties brokerConfig = parseConfigFile();
    Logger.info("Connecting to broker...");
    ConnectionFactory cf = new ConnectionFactory();
    ConnectionFactory scf = new ConnectionFactory();
    if (!brokerConfig.containsKey("broker_url") || !brokerConfig.containsKey("exchange")) {
      Logger.error("Missing broker url configuration.");
      System.exit(1);
    }
    try {

      Logger.info("Connecting to: " + brokerConfig.getProperty("broker_url"));
      cf.setUri(brokerConfig.getProperty("broker_url"));
      northConnection = cf.newConnection();
      northChannel = northConnection.createChannel();
      northExchangeName = brokerConfig.getProperty("exchange");
      northChannel.exchangeDeclare(northExchangeName, "topic");
      northQueueName = northExchangeName + "." + "InfraAbstract";
      northChannel.queueDeclare(northQueueName, true, false, false, null);
      Logger.info("Binding queue to topics...");

      northChannel.queueBind(northQueueName, northExchangeName, "platform.management.plugin.register");
      Logger.info("Bound to topic \"platform.platform.management.plugin.register\"");

      northChannel.queueBind(northQueueName, northExchangeName, "platform.management.plugin.deregister");
      Logger.info("Bound to topic \"platform.platform.management.plugin.deregister\"");

      northChannel.queueBind(northQueueName, northExchangeName, "infrastructure.#");
      Logger.info("[northbound] RabbitMqConsumer - bound to topic \"infrastructure.#\"");


      Logger.info("Connecting to: " + brokerConfig.getProperty("broker_url"));
      scf.setUri(brokerConfig.getProperty("broker_url"));
      southConnection = scf.newConnection();
      southChannel = southConnection.createChannel();
      southExchangeName = brokerConfig.getProperty("exchange") + "-" + "ia";
      southChannel.exchangeDeclare(southExchangeName, "topic");
      southQueueName = southExchangeName + "." + "Wrappers";
      southChannel.queueDeclare(southQueueName, true, false, false, null);
      Logger.info("Binding queue to topics...");

      //southChannel.queueBind(southQueueName, southExchangeName, "platform.management.plugin.register");
      //Logger.info("Bound to topic \"platform.platform.management.plugin.register\"");

      //southChannel.queueBind(southQueueName, southExchangeName, "platform.management.plugin.deregister");
      //Logger.info("Bound to topic \"platform.platform.management.plugin.deregister\"");

      southChannel.queueBind(southQueueName, southExchangeName, "infrastructure.#");
      Logger.info("[southbound] RabbitMqConsumer - bound to topic \"infrastructure.#\"");


    } catch (TimeoutException e) {
      Logger.error(e.getMessage(), e);
    } catch (KeyManagementException e) {
      Logger.error(e.getMessage(), e);
    } catch (NoSuchAlgorithmException e) {
      Logger.error(e.getMessage(), e);
    } catch (URISyntaxException e) {
      Logger.error(e.getMessage(), e);
    } catch (IOException e) {
      Logger.error(e.getMessage(), e);
    }

  }

  public Channel getNorthChannel() {
    return this.northChannel;
  }

  public Channel getSouthChannel() {
    return this.southChannel;
  }

  public String getNorthExchangeName() {
    return northExchangeName;
  }

  public String getSouthExchangeName() {
    return southExchangeName;
  }

  public String getNorthQueueName() {
    return northQueueName;
  }

  public String getSouthQueueName() {
    return southQueueName;
  }

  /**
   * Utility function to parse the broker configuration file.
   *
   * @return a Java Properties object representing the json config as a Key-Value map
   */
  private Properties parseConfigFile() {
    Properties prop = new Properties();
    try {
      InputStreamReader in =
          new InputStreamReader(new FileInputStream(configFilePath), Charset.forName("UTF-8"));

      JSONTokener tokener = new JSONTokener(in);

      JSONObject jsonObject = (JSONObject) tokener.nextValue();

      String brokerUrl = jsonObject.getString("broker_url");
      String exchange = jsonObject.getString("exchange");
      prop.put("broker_url", brokerUrl);
      prop.put("exchange", exchange);
    } catch (FileNotFoundException e) {
      Logger.error("Unable to load Broker Config file", e);
      System.exit(1);
    }

    return prop;
  }

}
