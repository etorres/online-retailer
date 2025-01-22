package es.eriktorr
package commons.std

import cats.effect.{Resource, Sync}
import io.hypersistence.tsid.TSID

trait TSIDGen[F[_]] extends AnyRef:
  def randomTSID: F[TSID]

object TSIDGen:
  def apply[F[_]](using ev: Sync[F]): TSIDGen[F] = new TSIDGen[F]:
    override def randomTSID: F[TSID] = ev.blocking(TSID.Factory.getTsid.nn)

  def resource[F[_]](using ev: Sync[F]): Resource[F, TSIDGen[F]] = Resource.pure(TSIDGen[F])
