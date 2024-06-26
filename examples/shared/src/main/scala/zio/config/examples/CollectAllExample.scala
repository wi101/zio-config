package zio.config.examples

import zio.IO
import zio.config._

import ConfigDescriptor._

/**
 * This is only an example of a working pattern that reads the environment variables to form a `List[A]`,
 * to show how the combinator `Config.collectAll` (Sequence)  can be helpful.
 *
 * This is not showing a standard pattern that user has to follow. It is up to the user to design the pattern
 * of key value pairs, and use the right combinators in the library to retrieve it purely and safely integrated with ZIO.
 */
final case class Variables(variable1: Int, variable2: Option[Int])

object CollectAllExample extends App {
  val listOfConfig: List[ConfigDescriptor[Variables]] =
    List("GROUP1", "GROUP2", "GROUP3", "GROUP4")
      .map(group => (int(s"${group}_VARIABLE1") zip int(s"${group}_VARIABLE2").optional).to[Variables])

  val configOfList: ConfigDescriptor[List[Variables]] =
    collectAll(listOfConfig.head, listOfConfig.tail: _*)

  val map: Map[String, String] =
    Map(
      "GROUP1_VARIABLE1" -> "1",
      "GROUP1_VARIABLE2" -> "2",
      "GROUP2_VARIABLE1" -> "3",
      "GROUP2_VARIABLE2" -> "4",
      "GROUP3_VARIABLE1" -> "5",
      "GROUP3_VARIABLE2" -> "6",
      "GROUP4_VARIABLE1" -> "7"
    )

  // loadOrThrow here is only for the purpose of example
  val resultZIO: IO[ReadError[String], List[Variables]] = read(configOfList from ConfigSource.fromMap(map, "constant"))
  val result                                            = resultZIO.unsafeRun

  val written: PropertyTree[String, String] = write(configOfList, result).getOrElse(throw new Exception("write failed"))

  assert(
    result == List(Variables(1, Some(2)), Variables(3, Some(4)), Variables(5, Some(6)), Variables(7, None))
  )

  assert(
    written.flattenString() ==
      Map(
        "GROUP3_VARIABLE1" -> List("5"),
        "GROUP3_VARIABLE2" -> List("6"),
        "GROUP1_VARIABLE2" -> List("2"),
        "GROUP1_VARIABLE1" -> List("1"),
        "GROUP2_VARIABLE2" -> List("4"),
        "GROUP2_VARIABLE1" -> List("3"),
        "GROUP4_VARIABLE1" -> List("7")
      )
  )

  // Read it back from the wrtten tree
  assert(
    read(configOfList from ConfigSource.fromPropertyTree(written, "tree"))
      equalM
        ::(Variables(1, Some(2)), List(Variables(3, Some(4)), Variables(5, Some(6)), Variables(7, None)))
  )

//   // Read it back from the flattened tree
  assert(
    read(configOfList from ConfigSource.fromMultiMap(written.flattenString(), "tree"))
      equalM
        ::(Variables(1, Some(2)), List(Variables(3, Some(4)), Variables(5, Some(6)), Variables(7, None)))
  )
}
