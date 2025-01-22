package es.eriktorr
package commons.spec

import org.scalacheck.Gen

object CollectionGenerators:
  private val defaultMaximumRepeatedTries: Int = 10

  /** Generator of a list of Ts
    *
    * @param num
    *   Number of distinct elements to generate
    * @param genElem
    *   Candidates generator
    * @param discardPredicate
    *   Function to discard elements. This helps to generate distinct elements that are also
    *   distinct from previously generated elements, for instance. Example:
    *   {{{
    *     for {
    *       firstElements <- nDistinct(5, Gen.choose(1, 100))
    *       secondElements <- nDistinct(5, Gen.choose(1, 100)) { element =>
    *         ! firstElements.contains(element)
    *         }
    *     } yield (firstElements, secondElements)
    *   }}}
    * @param maximumRepeatedTries
    *   This generator will fail if we generate maximumRepeatedTries repeated elements
    * @tparam T
    *   Type of elements
    * @return
    *   A generator of lists with num different Ts
    */
  def nDistinct[T](
      num: Int,
      genElem: Gen[T],
      discardPredicate: T => Boolean = (_: T) => false,
      maximumRepeatedTries: Int = defaultMaximumRepeatedTries,
  ): Gen[List[T]] =
    type State = (Set[T], Int)

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def go(state: State): Gen[Either[State, List[T]]] = state match
      case (_, retries) if retries == maximumRepeatedTries =>
        // Throws exception to improve error location
        throw RuntimeException(
          s"Generator failed after generating ${maximumRepeatedTries.toString} repeated values",
        )
      case (accumulated, _) if accumulated.size == num => Gen.const(Right(accumulated.toList))
      case (accumulated, retries) =>
        genElem.map { candidate =>
          if accumulated.contains(candidate) || discardPredicate(candidate) then
            Left((accumulated, retries + 1))
          else Left((accumulated + candidate, retries))
        }

    Gen.tailRecM((Set.empty[T], 0))(go)

  /** Generator of a list of Ts not already in `notIn`.
    *
    * @param num
    *   Number of distinct elements to generate
    * @param genElem
    *   Candidates generator
    * @param notIn
    *   Elements to discard in case we generate them
    * @param maximumRepeatedTries
    *   This generator will fail if we generate maximumRepeatedTries repeated elements
    * @tparam T
    *   Type of elements
    * @return
    *   A generator of lists with num different Ts. Example:
    *   {{{
    *               for {
    *                goodAccountIds <- nDistinct(5, accountIdGen)
    *                badAccountIds <- nDistinctExcluding(5, accountIdGen, goodAccountIds)
    *              } yield (goodAccountIds, badAccountIds)
    *   }}}
    */
  def nDistinctExcluding[T](
      num: Int,
      genElem: Gen[T],
      notIn: Iterable[T] = List.empty[T],
      maximumRepeatedTries: Int = defaultMaximumRepeatedTries,
  ): Gen[List[T]] =
    val notInset: Set[T] = notIn.toSet
    nDistinct(
      num,
      genElem,
      discardPredicate = notInset.contains,
      maximumRepeatedTries = maximumRepeatedTries,
    )
