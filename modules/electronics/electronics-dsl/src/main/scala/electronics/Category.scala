package es.eriktorr
package electronics

import cats.Order

/** Types of electronic devices.
  *
  * @param family
  *   The general family.
  * @see
  *   [[https://en.wikipedia.org/wiki/Portal:Electronics/Categories Electronics Categories]]
  */
enum Category(
    val family: Category.Family,
    val name: String,
    val aka: List[String] = List.empty,
):
  case Earphones extends Category(Category.Family.Audio, "Earphones", List("Headphones", "Headset"))
  case ElectricGuitar extends Category(Category.Family.Audio, "Electric guitar", List("Guitar"))
  case Microphone extends Category(Category.Family.Audio, "Microphone")
  case Speakers extends Category(Category.Family.Audio, "Speakers", List("Bluetooth speakers"))

  case CurlingIron extends Category(Category.Family.Beauty, "Curling iron")
  case ElectricRazor extends Category(Category.Family.Beauty, "Electric razor")
  case HairDryer extends Category(Category.Family.Beauty, "Hair dryer")

  case ClothesDryer extends Category(Category.Family.HouseholdAppliances, "Clothes dryer")
  case DishWasher extends Category(Category.Family.HouseholdAppliances, "Dish washer")
  case Iron extends Category(Category.Family.HouseholdAppliances, "Iron")
  case Lamp extends Category(Category.Family.HouseholdAppliances, "Lamp", List("Reading lamp"))
  case Refrigerator
      extends Category(Category.Family.HouseholdAppliances, "Refrigerator", List("Freezer"))
  case RoboticVacuumCleaner
      extends Category(
        Category.Family.HouseholdAppliances,
        "Robotic vacuum cleaner",
        List("Vacuum cleaner"),
      )
  case WashingMachine extends Category(Category.Family.HouseholdAppliances, "Washing machine")
  case WaterPurifier extends Category(Category.Family.HouseholdAppliances, "Water purifier")
  case WaterHeater extends Category(Category.Family.HouseholdAppliances, "Water heater")

  case Computer extends Category(Category.Family.Informatics, "Computer")
  case ExternalHardDrive extends Category(Category.Family.Informatics, "External hard drive")
  case GameController extends Category(Category.Family.Informatics, "Game controller")
  case Monitor extends Category(Category.Family.Informatics, "Monitor")
  case Mouse extends Category(Category.Family.Informatics, "Mouse")
  case Printer
      extends Category(
        Category.Family.Informatics,
        "Printer",
        List("Laser printer", "Inkjet Printer"),
      )
  case Projector extends Category(Category.Family.Informatics, "Projector")
  case Smartphone extends Category(Category.Family.Informatics, "Smartphone", List("Phone"))
  case Tablet extends Category(Category.Family.Informatics, "Tablet")
  case USBDrive extends Category(Category.Family.Informatics, "USB drive")

  case Blender extends Category(Category.Family.Kitchen, "Blender")
  case BreadMaker extends Category(Category.Family.Kitchen, "Bread maker")
  case CoffeeMaker extends Category(Category.Family.Kitchen, "Coffee maker")
  case ElectricGrill extends Category(Category.Family.Kitchen, "Electric grill")
  case ElectricFryingPan extends Category(Category.Family.Kitchen, "Electric frying pan")
  case ElectricStove extends Category(Category.Family.Kitchen, "Electric stove")
  case Juicer extends Category(Category.Family.Kitchen, "Juicer")
  case Kettle extends Category(Category.Family.Kitchen, "Kettle")
  case KitchenScale extends Category(Category.Family.Kitchen, "Kitchen scale", List("Scale"))
  case Microwave extends Category(Category.Family.Kitchen, "Microwave")
  case Mixer extends Category(Category.Family.Kitchen, "Mixer")
  case OilFreeFryer extends Category(Category.Family.Kitchen, "Oil-free fryer")
  case Oven extends Category(Category.Family.Kitchen, "Oven")
  case PressureCooker extends Category(Category.Family.Kitchen, "Pressure cooker")
  case RiceCooker extends Category(Category.Family.Kitchen, "Rice cooker")
  case Toaster extends Category(Category.Family.Kitchen, "Toaster")

  case ElectricDrill extends Category(Category.Family.Tools, "Electric drill", List("Drill"))

  case AirPurifier extends Category(Category.Family.Ventilation, "Air purifier")
  case AirConditioner
      extends Category(Category.Family.Ventilation, "Air conditioner", List("A/C", "air con"))
  case ExhaustFan extends Category(Category.Family.Ventilation, "Exhaust fan")
  case Fan extends Category(Category.Family.Ventilation, "Fan", List("Ceiling fan", "Wall fan"))
  case Radiator extends Category(Category.Family.Ventilation, "Radiator")

  case DigitalCamera extends Category(Category.Family.Video, "Digital camera", List("Camera"))
  case DoorBellCamera extends Category(Category.Family.Video, "Door bell camera")
  case RemoteControl extends Category(Category.Family.Video, "Remote control", List("TV control"))
  case SmartTV extends Category(Category.Family.Video, "Smart TV", List("Television", "TV"))
  case Webcam extends Category(Category.Family.Video, "Webcam")
  case WiFiModem extends Category(Category.Family.Video, "WiFi modem")

object Category:
  enum Family:
    case Audio, Beauty, HouseholdAppliances, Informatics, Kitchen, Tools, Ventilation, Video

  def option(value: String): Option[Category] =
    Category.values.find(_.toString.equalsIgnoreCase(value))

  given Order[Category] = Order.by[Category, String](_.toString)
