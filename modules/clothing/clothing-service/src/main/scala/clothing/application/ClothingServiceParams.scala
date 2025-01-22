package es.eriktorr
package clothing.application

import com.monovore.decline.Opts

final case class ClothingServiceParams(verbose: Boolean)

object ClothingServiceParams:
  def opts: Opts[ClothingServiceParams] = Opts
    .flag("verbose", short = "v", help = "Print extra metadata to the logs.")
    .orFalse
    .map(ClothingServiceParams.apply)
