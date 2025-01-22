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

  val max: Money = euroContext.defaultCurrency(10_000d)
