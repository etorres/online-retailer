package es.eriktorr
package clothing

import clothing.FakeClothingClient.ClothingClientState
import clothing.protobuf.{ClothingRequest, GetGarmentRequest}

import cats.data.OptionT
import cats.effect.{IO, Ref}

final class FakeClothingClient(stateRef: Ref[IO, ClothingClientState]) extends ClothingClient[IO]:
  override def getGarment(request: GetGarmentRequest): OptionT[IO, Garment] = OptionT(
    stateRef.get.map(_.garments.find(_.sku == request.sku)),
  )
  override def findGarmentsBy(request: ClothingRequest): IO[List[Garment]] =
    stateRef.get.map(_.garments)

object FakeClothingClient:
  final case class ClothingClientState(garments: List[Garment]):
    def set(newGarments: List[Garment]): ClothingClientState = copy(newGarments)

  object ClothingClientState:
    val empty: ClothingClientState = ClothingClientState(List.empty)
