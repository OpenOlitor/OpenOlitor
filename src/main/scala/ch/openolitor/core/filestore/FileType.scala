package ch.openolitor.core.filestore

sealed trait FileType {
  val bucket: FileStoreBucket
}

case object VorlageRechnung extends FileType { val bucket = VorlagenBucket }
case object VorlageEtikette extends FileType { val bucket = VorlagenBucket }
case object VorlageMahnung extends FileType { val bucket = VorlagenBucket }
case object VorlageBestellung extends FileType { val bucket = VorlagenBucket }
case object GeneriertRechnung extends FileType { val bucket = GeneriertBucket }
case object GeneriertEtikette extends FileType { val bucket = GeneriertBucket }
case object GeneriertMahnung extends FileType { val bucket = GeneriertBucket }
case object GeneriertBestellung extends FileType { val bucket = GeneriertBucket }
case object ProjektStammdaten extends FileType { val bucket = StammdatenBucket }
case object EsrDaten extends FileType { val bucket = EsrBucket }
case object UnknownFileType extends FileType { lazy val bucket = sys.error("This FileType has no bucket") }

object FileType {
  val AllFileTypes = List(
    VorlageRechnung,
    VorlageEtikette,
    VorlageMahnung,
    VorlageBestellung,
    GeneriertRechnung,
    GeneriertEtikette,
    GeneriertMahnung,
    GeneriertBestellung,
    ProjektStammdaten,
    EsrDaten)

  def apply(value: String): FileType = {
    AllFileTypes.find(_.toString.toLowerCase == value.toLowerCase).getOrElse(UnknownFileType)
  }
}