/* NSC -- new Scala compiler
 * Copyright 2005-2013 LAMP/EPFL
 * @author  Martin Odersky
 */

package scala.tools.nsc
package fsc

//import scala.tools.nsc.{AbstractScriptRunner, ScriptRunner, GenericRunnerSettings, Settings}
import scala.reflect.io.Path
import scala.util.control.NonFatal

class ResidentScriptRunner(settings: GenericRunnerSettings) extends AbstractScriptRunner(settings) with HasCompileSocket {
  lazy val compileSocket = CompileSocket

  /** Compile a script using the fsc compilation daemon.
   */
  protected def doCompile(scriptFile: String) = {
    val scriptPath       = Path(scriptFile).toAbsolute.path
    val compSettingNames = new Settings(msg => throw new RuntimeException(msg)).visibleSettings.toList map (_.name)
    val compSettings     = settings.visibleSettings.toList filter (compSettingNames contains _.name)
    val coreCompArgs     = compSettings flatMap (_.unparse)
    val compArgs         = coreCompArgs ++ List("-Xscript", mainClass, scriptPath)

    // TODO: untangle this mess of top-level objects with their own little view of the mutable world of settings
    compileSocket.verbose = settings.verbose.value

    compileSocket getOrCreateSocket "" match {
      case Some(sock) => compileOnServer(sock, compArgs)
      case _          => false
    }
  }
}

final class DaemonKiller(settings: GenericRunnerSettings) extends ScriptRunner {
  def runScript(script: String, scriptArgs: List[String]) = shutdownDaemon()

  def runScriptText(script: String, scriptArgs: List[String]) = shutdownDaemon()

  private def shutdownDaemon() =
    try {
      new StandardCompileClient().process(Array("-shutdown"))
      None
    } catch {
      case NonFatal(t) => Some(t)
    }
}
