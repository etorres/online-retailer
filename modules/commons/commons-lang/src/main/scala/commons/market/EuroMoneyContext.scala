package es.eriktorr
package commons.market

import squants.market.{EUR, GBP, Money, MoneyContext, USD}

object EuroMoneyContext:
  given euroContext: MoneyContext =
    MoneyContext(
      defaultCurrency = EUR,
      currencies = Set(EUR, GBP, USD),
      rates = List(EUR / GBP(0.84d), EUR / USD(1.03d)),
    )

  /** Maximum money amount that can be handled by this application.
    *
    * Money is represented in the database with the SQL data type `NUMERIC(9,5)`. This precision
    * allows a maximum possible value of 10000.
    */
  val max: Money = euroContext.defaultCurrency(10_000d)
