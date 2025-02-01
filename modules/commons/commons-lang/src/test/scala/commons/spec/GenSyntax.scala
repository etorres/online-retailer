package es.eriktorr
package commons.spec

import org.scalacheck.Gen
import org.scalacheck.Gen.Parameters
import org.scalacheck.rng.Seed

object GenSyntax:
  extension [A](self: Gen[A])
    def sampleWithSeed(seed: Option[Seed] = None, verbose: Boolean = true): A =
      val sampleSeed = seed.getOrElse(Seed.random())
      if verbose then println(s"Sampling with: ${sampleSeed.toString}")
      self.pureApply(Parameters.default.withNoInitialSeed, sampleSeed)
