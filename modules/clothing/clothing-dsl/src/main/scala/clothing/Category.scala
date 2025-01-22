package es.eriktorr
package clothing

import cats.Order

/** Types of clothing.
  *
  * @param family
  *   The general family.
  * @see
  *   [[https://www.minimizemymess.com/blog/types-of-clothing 215+ Types of Clothes]]
  */
enum Category(
    val family: Category.Family,
    val name: String,
    val aka: List[String] = List.empty,
):
  case Bodysuit extends Category(Category.Family.Tops, "Bodysuit")
  case Hoodie extends Category(Category.Family.Tops, "Hoodie")
  case Shirt extends Category(Category.Family.Tops, "Shirt", List("Blouse"))
  case Sleeveless
      extends Category(
        Category.Family.Tops,
        "Sleeveless",
        List("Tank top", "Vest"),
      )
  case Sweater
      extends Category(
        Category.Family.Tops,
        "Sweater",
        List("Jumper", "Jersey"),
      )
  case TShirt extends Category(Category.Family.Tops, "T-shirt")
  case Turtleneck extends Category(Category.Family.Tops, "Turtleneck")

  case CargoPants extends Category(Category.Family.Bottoms, "Cargo pants")
  case Jeans extends Category(Category.Family.Bottoms, "Jeans")
  case Leggings extends Category(Category.Family.Bottoms, "Leggings")
  case Slacks extends Category(Category.Family.Bottoms, "Slacks", List("Pants", "Trousers"))
  case TrackPants
      extends Category(
        Category.Family.Bottoms,
        "Track pants",
        List("Sweatpants", "Joggers"),
      )
  case Shorts
      extends Category(
        Category.Family.Bottoms,
        "Shorts",
        List("Hot pants", "Bermuda shorts"),
      )
  case Skirts
      extends Category(
        Category.Family.Bottoms,
        "Skirts",
        List("Miniskirt", "Tutu"),
      )

  case Coveralls extends Category(Category.Family.OnePiece, "Coveralls")
  case Dresses
      extends Category(
        Category.Family.OnePiece,
        "Dresses",
        List("Maxi dress", "Midi dress", "Wedding dress", "Winter dress"),
      )
  case Kimono extends Category(Category.Family.OnePiece, "Kimono")
  case Suit extends Category(Category.Family.OnePiece, "Suit", List("Tuxedo"))
  case Overalls extends Category(Category.Family.OnePiece, "Overalls")

  case Blazer extends Category(Category.Family.Outwear, "Blazer")
  case FurCoat extends Category(Category.Family.Outwear, "Fur coat")
  case Jacket
      extends Category(
        Category.Family.Outwear,
        "Jacket",
        List(
          "Anorak",
          "Bomber jacket",
          "Denim jacket",
          "Leather jacket",
          "Military jacket",
          "Parka",
          "Puffer jacket",
          "Rain jacket",
          "Trench coat",
          "Windbreaker",
        ),
      )

  case Bloomers extends Category(Category.Family.Intimates, "Bloomers")
  case Bras extends Category(Category.Family.Intimates, "Bras")
  case Housecoat
      extends Category(Category.Family.Intimates, "Housecoat", List("Dressing gown", "Bath robe"))
  case Pantyhose extends Category(Category.Family.Intimates, "Pantyhose", List("Tights"))
  case Pajamas extends Category(Category.Family.Intimates, "Pajamas")
  case Socks extends Category(Category.Family.Intimates, "Socks")
  case Underwear
      extends Category(
        Category.Family.Intimates,
        "Underwear",
        List("Boxer shorts", "Briefs", "Knickers"),
      )
  case Vest extends Category(Category.Family.Intimates, "Vest")

  case Boots extends Category(Category.Family.Footwear, "Boots")
  case FlipFlops extends Category(Category.Family.Footwear, "Flip flops")
  case Heels extends Category(Category.Family.Footwear, "Heels", List("Pumps"))
  case Loafers extends Category(Category.Family.Footwear, "Loafers")
  case Sandals extends Category(Category.Family.Footwear, "Sandals")
  case Slippers extends Category(Category.Family.Footwear, "Slippers")
  case Sneakers
      extends Category(
        Category.Family.Footwear,
        "Sneakers",
        List("Trainers", "Runners", "Running shoes"),
      )

  case Backpack extends Category(Category.Family.Accessories, "Backpack")
  case BaseballCap extends Category(Category.Family.Accessories, "Baseball cap")
  case Belt extends Category(Category.Family.Accessories, "Belt")
  case BumBag extends Category(Category.Family.Accessories, "Bum bag", List("Fanny pack"))
  case Clutch extends Category(Category.Family.Accessories, "Clutch")
  case Handbag extends Category(Category.Family.Accessories, "Handbag", List("Purse"))
  case Hat extends Category(Category.Family.Accessories, "Hat")
  case Headband extends Category(Category.Family.Accessories, "Headband")
  case Gloves extends Category(Category.Family.Accessories, "Gloves")
  case Jewellery extends Category(Category.Family.Accessories, "Jewellery")
  case Mittens extends Category(Category.Family.Accessories, "Mittens")
  case Scarf extends Category(Category.Family.Accessories, "Scarf")
  case Sunglasses extends Category(Category.Family.Accessories, "Sunglasses")
  case Tie extends Category(Category.Family.Accessories, "Tie")
  case Umbrella extends Category(Category.Family.Accessories, "Umbrella")

  case SwimShorts extends Category(Category.Family.Other, "Swim shorts", List("Trunks"))
  case SwimSuit extends Category(Category.Family.Other, "Swim suit", List("Bikini"))

object Category:
  enum Family:
    case Tops, Bottoms, OnePiece, Outwear, Intimates, Footwear, Accessories, Other

  def option(value: String): Option[Category] =
    Category.values.find(_.toString.equalsIgnoreCase(value))

  given Order[Category] = Order.by[Category, String](_.toString)
