package es.eriktorr
package commons.spec

import munit.Tag

import scala.util.Properties

object TestFilters:
  val online: Tag = new Tag("online")

  private val envVarsName: String = "SBT_TEST_ENV_VARS"

  def isSbt: Boolean = Properties.envOrNone(envVarsName).nonEmpty
