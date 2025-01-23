package es.eriktorr
package stock.application

import com.monovore.decline.Opts

final case class StockServiceParams(verbose: Boolean)

object StockServiceParams:
  def opts: Opts[StockServiceParams] = Opts
    .flag("verbose", short = "v", help = "Print extra metadata to the logs.")
    .orFalse
    .map(StockServiceParams.apply)
