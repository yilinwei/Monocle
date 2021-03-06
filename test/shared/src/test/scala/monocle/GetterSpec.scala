package monocle

import cats.Semigroupal
import cats.arrow.{Arrow, Category, Choice, Compose, Profunctor}
import cats.data.{Chain, NonEmptyChain, NonEmptyList, NonEmptyVector}

import scala.collection.immutable

class GetterSpec extends MonocleSuite {
  case class Bar(i: Int)
  case class Foo(bar: Bar)

  val bar = Getter[Foo, Bar](_.bar)
  val i   = Getter[Bar, Int](_.i)

  // test implicit resolution of type classes

  test("Getter has a Compose instance") {
    assertEquals(Compose[Getter].compose(i, bar).get(Foo(Bar(3))), 3)
  }

  test("Getter has a Category instance") {
    assertEquals(Category[Getter].id[Int].get(3), 3)
  }

  test("Getter has a Choice instance") {
    assertEquals(
      Choice[Getter]
        .choice(i, Choice[Getter].id[Int])
        .get(Left(Bar(3))),
      3
    )
  }

  test("Getter has a Profunctor instance") {
    assertEquals(Profunctor[Getter].rmap(bar)(_.i).get(Foo(Bar(3))), 3)
  }

  test("Getter has a Arrow instance") {
    assertEquals(Arrow[Getter].lift((_: Int) * 2).get(4), 8)
  }

  test("Getter has a Semigroupal instance") {
    val length = Getter[String, Int](_.length)
    val upper  = Getter[String, String](_.toUpperCase)
    assertEquals(
      Semigroupal[Getter[String, *]]
        .product(length, upper)
        .get("helloworld"),
      ((10, "HELLOWORLD"))
    )
  }

  test("get") {
    assertEquals(i.get(Bar(5)), 5)
  }

  test("find") {
    assertEquals(i.find(_ > 5)(Bar(9)), Some(9))
    assertEquals(i.find(_ > 5)(Bar(3)), None)
  }

  test("exist") {
    assertEquals(i.exist(_ > 5)(Bar(9)), true)
    assertEquals(i.exist(_ > 5)(Bar(3)), false)
  }

  test("zip") {
    val length = Getter[String, Int](_.length)
    val upper  = Getter[String, String](_.toUpperCase)
    assertEquals(length.zip(upper).get("helloworld"), ((10, "HELLOWORLD")))
  }

  test("to") {
    assertEquals(i.to(_.toString()).get(Bar(5)), "5")
  }

  test("some") {
    case class SomeTest(x: Int, y: Option[Int])
    val obj = SomeTest(1, Some(2))

    val getter = Getter((_: SomeTest).y)

    assertEquals(getter.some.getAll(obj), List(2))
    assertEquals(obj.optics.andThen(getter).some.getAll, List(2))
  }

  test("withDefault") {
    case class SomeTest(x: Int, y: Option[Int])
    val objSome = SomeTest(1, Some(2))
    val objNone = SomeTest(1, None)

    val getter = Getter((_: SomeTest).y)

    assertEquals(getter.withDefault(0).get(objSome), 2)
    assertEquals(getter.withDefault(0).get(objNone), 0)

    assertEquals(objSome.optics.andThen(getter).withDefault(0).get, 2)
    assertEquals(objNone.optics.andThen(getter).withDefault(0).get, 0)
  }

  test("each") {
    case class SomeTest(x: Int, y: List[Int])
    val obj = SomeTest(1, List(1, 2, 3))

    val getter = Getter((_: SomeTest).y)

    assertEquals(getter.each.getAll(obj), List(1, 2, 3))
    assertEquals(obj.optics.andThen(getter).each.getAll, List(1, 2, 3))
  }

  test("filter") {
    case class SomeTest(x: Int, y: Int)
    val obj = SomeTest(1, 2)

    val getter = Getter[SomeTest, Int](_.y)

    assertEquals(getter.filter(_ > 0).getAll(obj), List(2))
    assertEquals(obj.optics.andThen(getter).filter(_ > 0).getAll, List(2))
  }

  test("filterIndex") {
    case class SomeTest(x: Int, y: List[String])
    val obj = SomeTest(1, List("hello", "world"))

    val getter = Getter[SomeTest, List[String]](_.y)

    assertEquals(getter.filterIndex((_: Int) > 0).getAll(obj), List("world"))
    assertEquals(obj.optics.andThen(getter).filterIndex((_: Int) > 0).getAll, List("world"))
  }

  test("at") {
    val tuple2       = (1, 2)
    val tuple2Getter = Getter.id[(Int, Int)]
    assertEquals(tuple2Getter.at(1).get(tuple2), 1)
    assertEquals(tuple2Getter.at(2).get(tuple2), 2)
    assertEquals(tuple2.optics.andThen(tuple2Getter).at(1).get, 1)
    assertEquals(tuple2.optics.andThen(tuple2Getter).at(2).get, 2)

    val tuple3       = (1, 2, 3)
    val tuple3Getter = Getter.id[(Int, Int, Int)]
    assertEquals(tuple3Getter.at(1).get(tuple3), 1)
    assertEquals(tuple3Getter.at(2).get(tuple3), 2)
    assertEquals(tuple3Getter.at(3).get(tuple3), 3)
    assertEquals(tuple3.optics.andThen(tuple3Getter).at(1).get, 1)
    assertEquals(tuple3.optics.andThen(tuple3Getter).at(2).get, 2)
    assertEquals(tuple3.optics.andThen(tuple3Getter).at(3).get, 3)

    val tuple4       = (1, 2, 3, 4)
    val tuple4Getter = Getter.id[(Int, Int, Int, Int)]
    assertEquals(tuple4Getter.at(1).get(tuple4), 1)
    assertEquals(tuple4Getter.at(2).get(tuple4), 2)
    assertEquals(tuple4Getter.at(3).get(tuple4), 3)
    assertEquals(tuple4Getter.at(4).get(tuple4), 4)
    assertEquals(tuple4.optics.andThen(tuple4Getter).at(1).get, 1)
    assertEquals(tuple4.optics.andThen(tuple4Getter).at(2).get, 2)
    assertEquals(tuple4.optics.andThen(tuple4Getter).at(3).get, 3)
    assertEquals(tuple4.optics.andThen(tuple4Getter).at(4).get, 4)

    val tuple5       = (1, 2, 3, 4, 5)
    val tuple5Getter = Getter.id[(Int, Int, Int, Int, Int)]
    assertEquals(tuple5Getter.at(1).get(tuple5), 1)
    assertEquals(tuple5Getter.at(2).get(tuple5), 2)
    assertEquals(tuple5Getter.at(3).get(tuple5), 3)
    assertEquals(tuple5Getter.at(4).get(tuple5), 4)
    assertEquals(tuple5Getter.at(5).get(tuple5), 5)
    assertEquals(tuple5.optics.andThen(tuple5Getter).at(1).get, 1)
    assertEquals(tuple5.optics.andThen(tuple5Getter).at(2).get, 2)
    assertEquals(tuple5.optics.andThen(tuple5Getter).at(3).get, 3)
    assertEquals(tuple5.optics.andThen(tuple5Getter).at(4).get, 4)
    assertEquals(tuple5.optics.andThen(tuple5Getter).at(5).get, 5)

    val tuple6       = (1, 2, 3, 4, 5, 6)
    val tuple6Getter = Getter.id[(Int, Int, Int, Int, Int, Int)]
    assertEquals(tuple6Getter.at(1).get(tuple6), 1)
    assertEquals(tuple6Getter.at(2).get(tuple6), 2)
    assertEquals(tuple6Getter.at(3).get(tuple6), 3)
    assertEquals(tuple6Getter.at(4).get(tuple6), 4)
    assertEquals(tuple6Getter.at(5).get(tuple6), 5)
    assertEquals(tuple6Getter.at(6).get(tuple6), 6)
    assertEquals(tuple6.optics.andThen(tuple6Getter).at(1).get, 1)
    assertEquals(tuple6.optics.andThen(tuple6Getter).at(2).get, 2)
    assertEquals(tuple6.optics.andThen(tuple6Getter).at(3).get, 3)
    assertEquals(tuple6.optics.andThen(tuple6Getter).at(4).get, 4)
    assertEquals(tuple6.optics.andThen(tuple6Getter).at(5).get, 5)
    assertEquals(tuple6.optics.andThen(tuple6Getter).at(6).get, 6)

    val sortedMap       = immutable.SortedMap(1 -> "one")
    val sortedMapGetter = Getter.id[immutable.SortedMap[Int, String]]
    assertEquals(sortedMapGetter.at(1).get(sortedMap), Some("one"))
    assertEquals(sortedMapGetter.at(0).get(sortedMap), None)
    assertEquals(sortedMap.optics.andThen(sortedMapGetter).at(1).get, Some("one"))
    assertEquals(sortedMap.optics.andThen(sortedMapGetter).at(0).get, None)

    val listMap       = immutable.ListMap(1 -> "one")
    val listMapGetter = Getter.id[immutable.ListMap[Int, String]]
    assertEquals(listMapGetter.at(1).get(listMap), Some("one"))
    assertEquals(listMapGetter.at(0).get(listMap), None)
    assertEquals(listMap.optics.andThen(listMapGetter).at(1).get, Some("one"))
    assertEquals(listMap.optics.andThen(listMapGetter).at(0).get, None)

    val map       = immutable.Map(1 -> "one")
    val mapGetter = Getter.id[Map[Int, String]]
    assertEquals(mapGetter.at(1).get(map), Some("one"))
    assertEquals(mapGetter.at(0).get(map), None)
    assertEquals(map.optics.andThen(mapGetter).at(1).get, Some("one"))
    assertEquals(map.optics.andThen(mapGetter).at(0).get, None)

    val set       = Set(1)
    val setGetter = Getter.id[Set[Int]]
    assertEquals(setGetter.at(1).get(set), true)
    assertEquals(setGetter.at(0).get(set), false)
    assertEquals(set.optics.andThen(setGetter).at(1).get, true)
    assertEquals(set.optics.andThen(setGetter).at(0).get, false)
  }

  test("index") {
    val list       = List(1)
    val listGetter = Getter.id[List[Int]]
    assertEquals(listGetter.index(0).getAll(list), List(1))
    assertEquals(listGetter.index(1).getAll(list), Nil)
    assertEquals(list.optics.andThen(listGetter).index(0).getAll, List(1))
    assertEquals(list.optics.andThen(listGetter).index(1).getAll, Nil)

    val lazyList       = LazyList(1)
    val lazyListGetter = Getter.id[LazyList[Int]]
    assertEquals(lazyListGetter.index(0).getAll(lazyList), List(1))
    assertEquals(lazyListGetter.index(1).getAll(lazyList), Nil)
    assertEquals(lazyList.optics.andThen(lazyListGetter).index(0).getAll, List(1))
    assertEquals(lazyList.optics.andThen(lazyListGetter).index(1).getAll, Nil)

    val listMap       = immutable.ListMap(1 -> "one")
    val listMapGetter = Getter.id[immutable.ListMap[Int, String]]
    assertEquals(listMapGetter.index(0).getAll(listMap), Nil)
    assertEquals(listMapGetter.index(1).getAll(listMap), List("one"))
    assertEquals(listMap.optics.andThen(listMapGetter).index(0).getAll, Nil)
    assertEquals(listMap.optics.andThen(listMapGetter).index(1).getAll, List("one"))

    val map       = Map(1 -> "one")
    val mapGetter = Getter.id[Map[Int, String]]
    assertEquals(mapGetter.index(0).getAll(map), Nil)
    assertEquals(mapGetter.index(1).getAll(map), List("one"))
    assertEquals(map.optics.andThen(mapGetter).index(0).getAll, Nil)
    assertEquals(map.optics.andThen(mapGetter).index(1).getAll, List("one"))

    val sortedMap       = immutable.SortedMap(1 -> "one")
    val sortedMapGetter = Getter.id[immutable.SortedMap[Int, String]]
    assertEquals(sortedMapGetter.index(0).getAll(sortedMap), Nil)
    assertEquals(sortedMapGetter.index(1).getAll(sortedMap), List("one"))
    assertEquals(sortedMap.optics.andThen(sortedMapGetter).index(0).getAll, Nil)
    assertEquals(sortedMap.optics.andThen(sortedMapGetter).index(1).getAll, List("one"))

    val vector       = Vector(1)
    val vectorGetter = Getter.id[Vector[Int]]
    assertEquals(vectorGetter.index(0).getAll(vector), List(1))
    assertEquals(vectorGetter.index(1).getAll(vector), Nil)
    assertEquals(vector.optics.andThen(vectorGetter).index(0).getAll, List(1))
    assertEquals(vector.optics.andThen(vectorGetter).index(1).getAll, Nil)

    val chain       = Chain.one(1)
    val chainGetter = Getter.id[Chain[Int]]
    assertEquals(chainGetter.index(0).getAll(chain), List(1))
    assertEquals(chainGetter.index(1).getAll(chain), Nil)
    assertEquals(chain.optics.andThen(chainGetter).index(0).getAll, List(1))
    assertEquals(chain.optics.andThen(chainGetter).index(1).getAll, Nil)

    val nec       = NonEmptyChain.one(1)
    val necGetter = Getter.id[NonEmptyChain[Int]]
    assertEquals(necGetter.index(0).getAll(nec), List(1))
    assertEquals(necGetter.index(1).getAll(nec), Nil)
    assertEquals(nec.optics.andThen(necGetter).index(0).getAll, List(1))
    assertEquals(nec.optics.andThen(necGetter).index(1).getAll, Nil)

    val nev       = NonEmptyVector.one(1)
    val nevGetter = Getter.id[NonEmptyVector[Int]]
    assertEquals(nevGetter.index(0).getAll(nev), List(1))
    assertEquals(nevGetter.index(1).getAll(nev), Nil)
    assertEquals(nev.optics.andThen(nevGetter).index(0).getAll, List(1))
    assertEquals(nev.optics.andThen(nevGetter).index(1).getAll, Nil)

    val nel       = NonEmptyList.one(1)
    val nelGetter = Getter.id[NonEmptyList[Int]]
    assertEquals(nelGetter.index(0).getAll(nel), List(1))
    assertEquals(nelGetter.index(1).getAll(nel), Nil)
    assertEquals(nel.optics.andThen(nelGetter).index(0).getAll, List(1))
    assertEquals(nel.optics.andThen(nelGetter).index(1).getAll, Nil)
  }
}
