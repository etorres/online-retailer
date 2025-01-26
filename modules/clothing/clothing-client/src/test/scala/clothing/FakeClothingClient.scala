package es.eriktorr
package clothing

import clothing.FakeClothingClient.ClothingClientState
import clothing.protobuf.ClothingRequest

import cats.effect.{IO, Ref}

final class FakeClothingClient(stateRef: Ref[IO, ClothingClientState]) extends ClothingClient[IO]:
  override def findGarmentsBy(request: ClothingRequest): IO[List[Garment]] =
    stateRef.get.map(_.garments)

object FakeClothingClient:
  final case class ClothingClientState(garments: List[Garment]):
    def set(newGarments: List[Garment]): ClothingClientState = copy(newGarments)

  object ClothingClientState:
    val empty: ClothingClientState = ClothingClientState(List.empty)
