package es.eriktorr
package electronics.application

import com.monovore.decline.Opts

final case class ElectronicsServiceParams(verbose: Boolean)

object ElectronicsServiceParams:
  def opts: Opts[ElectronicsServiceParams] = Opts
    .flag("verbose", short = "v", help = "Print extra metadata to the logs.")
    .orFalse
    .map(ElectronicsServiceParams.apply)
