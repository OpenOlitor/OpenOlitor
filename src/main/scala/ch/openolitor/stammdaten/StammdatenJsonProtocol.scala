/*                                                                           *\
*    ____                   ____  ___ __                                      *
*   / __ \____  ___  ____  / __ \/ (_) /_____  _____                          *
*  / / / / __ \/ _ \/ __ \/ / / / / / __/ __ \/ ___/   OpenOlitor             *
* / /_/ / /_/ /  __/ / / / /_/ / / / /_/ /_/ / /       contributed by tegonal *
* \____/ .___/\___/_/ /_/\____/_/_/\__/\____/_/        http://openolitor.ch   *
*     /_/                                                                     *
*                                                                             *
* This program is free software: you can redistribute it and/or modify it     *
* under the terms of the GNU General Public License as published by           *
* the Free Software Foundation, either version 3 of the License,              *
* or (at your option) any later version.                                      *
*                                                                             *
* This program is distributed in the hope that it will be useful, but         *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY  *
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for *
* more details.                                                               *
*                                                                             *
* You should have received a copy of the GNU General Public License along     *
* with this program. If not, see http://www.gnu.org/licenses/                 *
*                                                                             *
\*                                                                           */
package ch.openolitor.stammdaten

import spray.json._
import ch.openolitor.core.models._
import java.util.UUID
import org.joda.time._
import org.joda.time.format._
import ch.openolitor.core.BaseJsonProtocol
import ch.openolitor.stammdaten.models._
import com.typesafe.scalalogging.LazyLogging
import ch.openolitor.core.JSONSerializable
import zangelo.spray.json.AutoProductFormats
import scala.collection.immutable.TreeMap

/**
 * JSON Format deklarationen für das Modul Stammdaten
 */
trait StammdatenJsonProtocol extends BaseJsonProtocol with LazyLogging with AutoProductFormats[JSONSerializable] {

  //enum formats
  implicit val wochentagFormat = enumFormat(x => Wochentag.apply(x).getOrElse(Montag))
  implicit val monatFormat = enumFormat(x => Monat.apply(x).getOrElse(Januar))
  implicit val rhythmusFormat = enumFormat(Rhythmus.apply)
  implicit val preiseinheitFormat = new JsonFormat[Preiseinheit] {
    def write(obj: Preiseinheit): JsValue =
      obj match {
        case ProLieferung => JsString("Lieferung")
        case ProMonat => JsString("Monat")
        case ProQuartal => JsString("Quartal")
        case ProJahr => JsString("Jahr")
        case ProAbo => JsString("Abo")
      }

    def read(json: JsValue): Preiseinheit =
      json match {
        case JsString("Lieferung") => ProLieferung
        case JsString("Quartal") => ProQuartal
        case JsString("Monat") => ProMonat
        case JsString("Jahr") => ProJahr
        case JsString("Abo") => ProAbo
        case pe => sys.error(s"Unknown Preiseinheit:$pe")
      }
  }

  implicit val fristeinheitFormat = new JsonFormat[Fristeinheit] {
    def write(obj: Fristeinheit): JsValue =
      obj match {
        case Wochenfrist => JsString("Wochen")
        case Monatsfrist => JsString("Monate")
      }

    def read(json: JsValue): Fristeinheit =
      json match {
        case JsString("Wochen") => Wochenfrist
        case JsString("Monate") => Monatsfrist
        case pe => sys.error(s"Unknown Fristeinheit:$pe")
      }
  }

  implicit val rolleFormat = new RootJsonFormat[Rolle] {
    def write(obj: Rolle): JsValue =
      obj match {
        case AdministratorZugang => JsString("Administrator")
        case KundenZugang => JsString("Kunde")
      }

    def read(json: JsValue): Rolle =
      json match {
        case JsString("Administrator") => AdministratorZugang
        case JsString("Kunde") => KundenZugang
        case pe => sys.error(s"Unknown Rolle:$pe")
      }
  }

  implicit val anredeFormat = new JsonFormat[Anrede] {
    def write(obj: Anrede): JsValue =
      obj match {
        case Herr => JsString("Herr")
        case Frau => JsString("Frau")
      }

    def read(json: JsValue): Anrede =
      json match {
        case JsString("Herr") => Herr
        case JsString("Frau") => Frau
        case pe => sys.error(s"Unknown Anrede:$pe")
      }
  }

  implicit val waehrungFormat = enumFormat(Waehrung.apply)
  implicit val laufzeiteinheitFormat = enumFormat(Laufzeiteinheit.apply)
  implicit val lieferungStatusFormat = enumFormat(LieferungStatus.apply)
  implicit val korbStatusFormat = enumFormat(KorbStatus.apply)
  implicit val auslieferungStatusFormat = enumFormat(AuslieferungStatus.apply)
  implicit val pendenzStatusFormat = enumFormat(PendenzStatus.apply)
  implicit val liefereinheitFormat = enumFormat(Liefereinheit.apply)

  //id formats
  implicit val vertriebIdFormat = baseIdFormat(VertriebId)
  implicit val vertriebsartIdFormat = baseIdFormat(VertriebsartId)
  implicit val abotypIdFormat = baseIdFormat(AbotypId)
  implicit val depotIdFormat = baseIdFormat(DepotId)
  implicit val tourIdFormat = baseIdFormat(TourId)
  implicit val auslieferungIdFormat = baseIdFormat(AuslieferungId)
  implicit val optionAuslieferungIdFormat = new OptionFormat[AuslieferungId]
  implicit val multiAuslieferungIdFormat = baseIdFormat(MultiAuslieferungId)
  implicit val kundeIdFormat = baseIdFormat(KundeId)
  implicit val pendenzIdFormat = baseIdFormat(PendenzId)
  implicit val aboIdFormat = baseIdFormat(AboId)
  implicit val lieferungIdFormat = baseIdFormat(LieferungId)
  implicit val lieferungOnLieferplanungIdFormat = baseIdFormat(LieferungOnLieferplanungId)
  implicit val lieferplanungIdFormat = baseIdFormat(LieferplanungId)
  implicit val lieferpositionIdFormat = baseIdFormat(LieferpositionId)
  implicit val bestellungIdFormat = baseIdFormat(BestellungId)
  implicit val sammelbestellungIdFormat = baseIdFormat(SammelbestellungId)
  implicit val bestellpositionIdFormat = baseIdFormat(BestellpositionId)
  implicit val customKundentypIdFormat = baseIdFormat(CustomKundentypId.apply)
  implicit val abwesenheitIdFormat = baseIdFormat(AbwesenheitId.apply)
  implicit val projektVorlageIdFormat = baseIdFormat(ProjektVorlageId.apply)
  implicit val kundentypIdFormat = new RootJsonFormat[KundentypId] {
    def write(obj: KundentypId): JsValue =
      JsString(obj.id)

    def read(json: JsValue): KundentypId =
      json match {
        case JsString(id) => KundentypId(id)
        case kt => sys.error(s"Unknown KundentypId:$kt")
      }
  }
  implicit val produktIdFormat = baseIdFormat(ProduktId.apply)
  implicit val optionProduktIdFormat = new OptionFormat[ProduktId]
  implicit val produktekategorieIdFormat = baseIdFormat(ProduktekategorieId.apply)
  implicit val baseProduktekategorieIdFormat = new JsonFormat[BaseProduktekategorieId] {
    def write(obj: BaseProduktekategorieId): JsValue =
      JsString(obj.id)

    def read(json: JsValue): BaseProduktekategorieId =
      json match {
        case JsString(id) => BaseProduktekategorieId(id)
        case kt => sys.error(s"Unknown BaseProduktekategorieId:$kt")
      }
  }
  implicit val produzentIdFormat = baseIdFormat(ProduzentId.apply)
  implicit val baseProduzentIdFormat = new JsonFormat[BaseProduzentId] {
    def write(obj: BaseProduzentId): JsValue =
      JsString(obj.id)

    def read(json: JsValue): BaseProduzentId =
      json match {
        case JsString(id) => BaseProduzentId(id)
        case kt => sys.error(s"Unknown BaseProduzentId:$kt")
      }
  }
  implicit val projektIdFormat = baseIdFormat(ProjektId.apply)
  implicit val korbIdFormat = baseIdFormat(KorbId.apply)
  implicit val einladungIdFormat = baseIdFormat(EinladungId.apply)

  implicit val lieferzeitpunktFormat = new RootJsonFormat[Lieferzeitpunkt] {
    def write(obj: Lieferzeitpunkt): JsValue =
      obj match {
        case w: Wochentag => w.toJson
        case _ => JsObject()
      }

    def read(json: JsValue): Lieferzeitpunkt =
      json.convertTo[Wochentag]
  }

  implicit val liefersaisonFormat = new RootJsonFormat[Liefersaison] {
    def write(obj: Liefersaison): JsValue =
      obj match {
        case m: Monat => m.toJson
        case _ => JsObject()
      }

    def read(json: JsValue): Liefersaison =
      json.convertTo[Monat]
  }

  implicit val vertriebsartDetailFormat = new RootJsonFormat[VertriebsartDetail] {
    def write(obj: VertriebsartDetail): JsValue =
      JsObject((obj match {
        case p: PostlieferungDetail => p.toJson
        case hl: HeimlieferungDetail => hl.toJson
        case dl: DepotlieferungDetail => dl.toJson
      }).asJsObject.fields + ("typ" -> JsString(obj.productPrefix.replaceAll("Detail", ""))))

    def read(json: JsValue): VertriebsartDetail =
      json.asJsObject.getFields("typ") match {
        case Seq(JsString("Postlieferung")) => json.convertTo[PostlieferungDetail]
        case Seq(JsString("Heimlieferung")) => json.convertTo[HeimlieferungDetail]
        case Seq(JsString("Depotlieferung")) => json.convertTo[DepotlieferungDetail]
      }
  }

  implicit val postlieferungModifyFormat = jsonFormat0(PostlieferungModify)
  implicit val depotlieferungModifyFormat = jsonFormat1(DepotlieferungModify)
  implicit val heimlieferungModifyFormat = jsonFormat1(HeimlieferungModify)

  implicit val vertriebsartModifyFormat = new RootJsonFormat[VertriebsartModify] {
    def write(obj: VertriebsartModify): JsValue =
      JsObject((obj match {
        case p: PostlieferungModify => p.toJson
        case hl: HeimlieferungModify => hl.toJson
        case dl: DepotlieferungModify => dl.toJson
      }).asJsObject.fields + ("typ" -> JsString(obj.productPrefix.replaceAll("Detail", ""))))

    def read(json: JsValue): VertriebsartModify = {
      json.asJsObject.getFields("typ") match {
        case Seq(JsString("Postlieferung")) => json.convertTo[PostlieferungModify]
        case Seq(JsString("Heimlieferung")) => json.convertTo[HeimlieferungModify]
        case Seq(JsString("Depotlieferung")) => json.convertTo[DepotlieferungModify]
      }
    }
  }

  implicit val postlieferungAbotypModifyFormat = jsonFormat1(PostlieferungAbotypModify)
  implicit val depotlieferungAbotypModifyFormat = jsonFormat2(DepotlieferungAbotypModify)
  implicit val heimlieferungAbotypModifyFormat = jsonFormat2(HeimlieferungAbotypModify)

  implicit val vertriebsartAbotypModifyFormat = new RootJsonFormat[VertriebsartAbotypModify] {
    def write(obj: VertriebsartAbotypModify): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): VertriebsartAbotypModify = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAbotypModify]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAbotypModify]
      } else {
        json.convertTo[PostlieferungAbotypModify]
      }
    }
  }

  implicit val auslieferungFormat = new RootJsonFormat[Auslieferung] {
    def write(obj: Auslieferung): JsValue =
      JsObject((obj match {
        case p: PostAuslieferung => p.toJson
        case t: TourAuslieferung => t.toJson
        case d: DepotAuslieferung => d.toJson
      }).asJsObject.fields + ("typ" -> JsString(obj.productPrefix)))

    def read(json: JsValue): Auslieferung = {
      json.asJsObject.getFields("typ") match {
        case Seq(JsString("PostAuslieferung")) => json.convertTo[PostAuslieferung]
        case Seq(JsString("TourAuslieferung")) => json.convertTo[TourAuslieferung]
        case Seq(JsString("DepotAuslieferung")) => json.convertTo[DepotAuslieferung]
      }
    }
  }

  // json formatter which adds calculated boolean field
  def enhanceWithBooleanFlag[E <: AktivRange](flag: String)(implicit defaultFormat: JsonFormat[E]): RootJsonFormat[E] = new RootJsonFormat[E] {
    def write(obj: E): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields +
        (flag -> JsBoolean(
          obj.aktiv
        )))
    }

    def read(json: JsValue): E = defaultFormat.read(json)
  }

  implicit val abotypFormat = enhanceWithBooleanFlag[Abotyp]("aktiv")

  implicit val systemKundentypFormat = new JsonFormat[SystemKundentyp] {
    def write(obj: SystemKundentyp): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): SystemKundentyp =
      json match {
        case JsString(kundentyp) => SystemKundentyp.parse(kundentyp).getOrElse(sys.error(s"Unknown System-Kundentyp:$kundentyp"))
        case pt => sys.error(s"Unknown personentyp:$pt")
      }
  }

  implicit val kundentypFormat = new JsonFormat[Kundentyp] {
    def write(obj: Kundentyp): JsValue =
      obj match {
        case s: SystemKundentyp => s.toJson
        case c: CustomKundentyp => c.toJson
      }

    def read(json: JsValue): Kundentyp =
      json match {
        case system: JsString => json.convertTo[SystemKundentyp]
        case custom: JsObject => json.convertTo[CustomKundentyp]
        case pt => sys.error(s"Unknown personentyp:$pt")
      }
  }

  implicit val treeMapIntFormat = new JsonFormat[TreeMap[String, Int]] {
    def write(obj: TreeMap[String, Int]): JsValue = {
      val elems = obj.toTraversable.map {
        case (key, value) => JsObject("key" -> JsString(key), "value" -> JsNumber(value))
      }.toVector
      JsArray(elems)
    }

    def read(json: JsValue): TreeMap[String, Int] =
      json match {
        case JsArray(elems) =>
          val entries = elems.map { elem =>
            elem.asJsObject.getFields("key", "value") match {
              case Seq(JsString(key), JsNumber(value)) =>
                (key -> value.toInt)
            }
          }.toSeq
          (TreeMap.empty[String, Int] /: entries) { (tree, c) => tree + c }
        case pt => sys.error(s"Unknown treemap:$pt")
      }
  }

  implicit val treeMapBigDecimalFormat = new JsonFormat[TreeMap[String, BigDecimal]] {
    def write(obj: TreeMap[String, BigDecimal]): JsValue = {
      val elems = obj.toTraversable.map {
        case (key, value) => JsObject("key" -> JsString(key), "value" -> JsNumber(value))
      }.toVector
      JsArray(elems)
    }

    def read(json: JsValue): TreeMap[String, BigDecimal] =
      json match {
        case JsArray(elems) =>
          val entries = elems.map { elem =>
            elem.asJsObject.getFields("key", "value") match {
              case Seq(JsString(key), JsNumber(value)) =>
                (key -> value.asInstanceOf[BigDecimal])
              case Seq(JsString(key), JsString(value)) =>
                (key -> BigDecimal(value))
            }
          }.toSeq
          (TreeMap.empty[String, BigDecimal] /: entries) { (tree, c) => tree + c }
        case pt => sys.error(s"Unknown treemap:$pt")
      }
  }

  implicit val vertriebFormat = autoProductFormat[Vertrieb]
  implicit val depotaboFormat = autoProductFormat[DepotlieferungAbo]
  implicit val depotaboDetailFormat = autoProductFormat[DepotlieferungAboDetail]
  implicit val depotaboModifyFormat = autoProductFormat[DepotlieferungAboModify]
  implicit val heimlieferungAboFormat = autoProductFormat[HeimlieferungAbo]
  implicit val heimlieferungAboDetailFormat = autoProductFormat[HeimlieferungAboDetail]
  implicit val heimlieferungAboModifyFormat = autoProductFormat[HeimlieferungAboModify]
  implicit val postlieferungAboFormat = autoProductFormat[PostlieferungAbo]
  implicit val postlieferungAboDetailFormat = autoProductFormat[PostlieferungAboDetail]
  implicit val postlieferungAboModifyFormat = autoProductFormat[PostlieferungAboModify]

  implicit val aboDetailFormat = new RootJsonFormat[AboDetail] {
    def write(obj: AboDetail): JsValue =
      obj match {
        case d: DepotlieferungAboDetail => d.toJson
        case h: HeimlieferungAboDetail => h.toJson
        case p: PostlieferungAboDetail => p.toJson
        case _ => JsObject()
      }

    def read(json: JsValue): AboDetail = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAboDetail]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAboDetail]
      } else {
        json.convertTo[PostlieferungAboDetail]
      }
    }
  }

  implicit val aboFormat = new RootJsonFormat[Abo] {
    def write(obj: Abo): JsValue =
      obj match {
        case d: DepotlieferungAbo => d.toJson
        case h: HeimlieferungAbo => h.toJson
        case p: PostlieferungAbo => p.toJson
        case _ => JsObject()
      }

    def read(json: JsValue): Abo = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAbo]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAbo]
      } else {
        json.convertTo[PostlieferungAbo]
      }
    }
  }

  implicit val aboModifyFormat = new RootJsonFormat[AboModify] {
    def write(obj: AboModify): JsValue =
      JsString(obj.productPrefix)

    def read(json: JsValue): AboModify = {
      logger.debug("Got new AboModify" + json.compactPrint)
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotlieferungAboModify]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[HeimlieferungAboModify]
      } else {
        json.convertTo[PostlieferungAboModify]
      }
    }
  }

  implicit val lieferungAbotypCreateFormat = autoProductFormat[LieferungAbotypCreate]
  implicit val lieferungModifyFormat = autoProductFormat[LieferungModify]
  implicit val lieferplanungModifyFormat = autoProductFormat[LieferplanungModify]
  implicit val lieferplanungCreateFormat = autoProductFormat[LieferplanungCreate]
  implicit val lieferpositionModifyFormat = autoProductFormat[LieferpositionModify]
  implicit val lieferpositionenCreateFormat = autoProductFormat[LieferpositionenModify]
  implicit val lieferungPlanungAddFormat = autoProductFormat[LieferungPlanungAdd]
  implicit val lieferungPlanungRemoveFormat = autoProductFormat[LieferungPlanungRemove]
  implicit val bestellungenCreateFormat = autoProductFormat[BestellungenCreate]
  implicit val bestellpositionModifyFormat = autoProductFormat[BestellpositionModify]

  implicit val korbCreateFormat = autoProductFormat[KorbCreate]

  implicit val projektModifyFormat = autoProductFormat[ProjektModify]

  // special report formats
  def enhancedProjektReportFormatDef(defaultFormat: JsonFormat[ProjektReport]): RootJsonFormat[ProjektReport] = new RootJsonFormat[ProjektReport] {
    def write(obj: ProjektReport): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields +
        (
          "strasseUndNummer" -> JsString(obj.strasseUndNummer.getOrElse("")),
          "plzOrt" -> JsString(obj.plzOrt.getOrElse("")),
          "adresszeilen" -> JsArray(obj.adresszeilen.map(JsString(_)).toVector)
        ))
    }

    def read(json: JsValue): ProjektReport = defaultFormat.read(json)
  }
  implicit val enhancedProjektReportFormat = enhancedProjektReportFormatDef(autoProductFormat[ProjektReport])

  def enhancedKundeReportFormatDef[K <: IKundeReport](defaultFormat: JsonFormat[K]): RootJsonFormat[K] = new RootJsonFormat[K] {
    def write(obj: K): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields +
        (
          "strasseUndNummer" -> JsString(obj.strasseUndNummer),
          "plzOrt" -> JsString(obj.plzOrt),
          "strasseUndNummerLieferung" -> JsString(obj.strasseUndNummerLieferung.getOrElse("")),
          "plzOrtLieferung" -> JsString(obj.plzOrtLieferung.getOrElse("")),
          "adresszeilen" -> JsArray(obj.adresszeilen.map(JsString(_)).toVector),
          "lieferAdresszeilen" -> JsArray(obj.lieferAdresszeilen.map(JsString(_)).toVector)
        ))
    }

    def read(json: JsValue): K = defaultFormat.read(json)
  }
  implicit val enhancedKundeReportFormat: RootJsonFormat[KundeReport] = enhancedKundeReportFormatDef(autoProductFormat[KundeReport])
  implicit val enhancedKundeDetailReportFormat: RootJsonFormat[KundeDetailReport] = enhancedKundeReportFormatDef(autoProductFormat[KundeDetailReport])
  implicit val korbReportFormat = autoProductFormat[KorbReport]

  def enhancedDepotReportFormatDef[D <: IDepotReport](defaultFormat: JsonFormat[D]): RootJsonFormat[D] = new RootJsonFormat[D] {
    def write(obj: D): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields +
        (
          "strasseUndNummer" -> JsString(obj.strasseUndNummer.getOrElse("")),
          "plzOrt" -> JsString(obj.plzOrt),
          "adresszeilen" -> JsArray(obj.adresszeilen.map(JsString(_)).toVector)
        ))
    }

    def read(json: JsValue): D = defaultFormat.read(json)
  }

  implicit val depotlieferungAboReportFormat = autoProductFormat[DepotlieferungAboReport]
  implicit val enhancedDepotReportFormat: RootJsonFormat[DepotReport] = enhancedDepotReportFormatDef(autoProductFormat[DepotReport])
  implicit val enhancedDepotDetailReportFormat: RootJsonFormat[DepotDetailReport] = enhancedDepotReportFormatDef(autoProductFormat[DepotDetailReport])

  def enhancedProduzentReportFormatDef[D <: IProduzentReport](defaultFormat: JsonFormat[D]): RootJsonFormat[D] = new RootJsonFormat[D] {
    def write(obj: D): JsValue = {
      JsObject(defaultFormat.write(obj)
        .asJsObject.fields +
        (
          "strasseUndNummer" -> JsString(obj.strasseUndNummer.getOrElse("")),
          "plzOrt" -> JsString(obj.plzOrt),
          "adresszeilen" -> JsArray(obj.adresszeilen.map(JsString(_)).toVector)
        ))
    }

    def read(json: JsValue): D = defaultFormat.read(json)
  }

  implicit val enhancedProduzentDetailReportFormat: RootJsonFormat[ProduzentDetailReport] = enhancedProduzentReportFormatDef(autoProductFormat[ProduzentDetailReport])

  implicit val depotAuslieferungReportFormat = autoProductFormat[DepotAuslieferungReport]
  implicit val tourAuslieferungReportFormat = autoProductFormat[TourAuslieferungReport]
  implicit val postAuslieferungReportFormat = autoProductFormat[PostAuslieferungReport]
  implicit val auslieferungReportFormat = new RootJsonFormat[AuslieferungReport] {
    def write(obj: AuslieferungReport): JsValue =
      obj match {
        case d: DepotAuslieferungReport => d.toJson
        case h: TourAuslieferungReport => h.toJson
        case p: PostAuslieferungReport => p.toJson
        case _ => JsObject()
      }

    def read(json: JsValue): AuslieferungReport = {
      if (!json.asJsObject.getFields("depotId").isEmpty) {
        json.convertTo[DepotAuslieferungReport]
      } else if (!json.asJsObject.getFields("tourId").isEmpty) {
        json.convertTo[TourAuslieferungReport]
      } else {
        json.convertTo[PostAuslieferungReport]
      }
    }
  }
}
