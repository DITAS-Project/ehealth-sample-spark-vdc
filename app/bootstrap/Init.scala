/**
 * Copyright 2018 IBM
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * This is being developed for the DITAS Project: https://www.ditas-project.eu/
 */
package bootstrap

import scala.concurrent.Future

import org.apache.spark.sql.SparkSession

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.Logger
import play.api.inject.ApplicationLifecycle

@Singleton
class Init @Inject() (lifecycle: ApplicationLifecycle, config: Configuration) {
  /**
    * On start load the  SparkSession
    */
  private var sparkSession = SparkSession.builder
    .master(config.get[String]("spark.master"))
    .appName(config.get[String]("spark.app.name"))
    .config("spark.jars", config.get[String]("spark.jars"))
    .config("spark.sql.shuffle.partitions", config.get[String]("spark.sql.shuffle.partitions"))
    .config("spark.hadoop.fs.s3a.endpoint", config.get[String]("spark.hadoop.fs.s3a.endpoint"))
    .config("spark.hadoop.fs.s3a.access.key", config.get[String]("spark.hadoop.fs.s3a.access.key"))
    .config("spark.hadoop.fs.s3a.secret.key", config.get[String]("spark.hadoop.fs.s3a.secret.key"))
    .config("spark.hadoop.fs.s3a.path.style.access", config.get[String]("spark.hadoop.fs.s3a.path.style.access"))
    .config("spark.hadoop.fs.s3a.impl", config.get[String]("spark.hadoop.fs.s3a.impl"))
    .config("spark.hadoop.fs.AbstractFileSystem.s3a.impl", config.get[String]("spark.hadoop.fs.AbstractFileSystem.s3a.impl"))
    .getOrCreate()

  private var debugMode = false
  private var showDataFrameLength = 10
  private var enforcementEngineURL = ""
  if (config.has("policy.enforcement.play.url")) {
    enforcementEngineURL = config.get[String]("policy.enforcement.play.url")
  }
  if (config.has("debug.mode")) {
    debugMode = config.get[Boolean]("debug.mode")
  }
  if (config.has("show.dataframe.len")) {
    showDataFrameLength = config.get[Int]("show.dataframe.len")
  }

  Logger.info("Starting VDCMethods application")

  lifecycle.addStopHook { () =>
    sparkSession.stop()
    Logger.info("Stopping VDCMethods application")
    Future.successful(sparkSession.stop())
  }

  def getSparkSessionInstance = {
    sparkSession
  }

  def getDebugMode = {
    debugMode
  }

  def getDfShowLen = {
    showDataFrameLength
  }

  def getEnforcementEngineURL = {
    enforcementEngineURL
  }
}
