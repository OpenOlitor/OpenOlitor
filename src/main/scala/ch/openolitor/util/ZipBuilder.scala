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
package ch.openolitor.util

import java.util.zip._
import java.io.ByteArrayOutputStream
import scala.util.Try
import java.io.InputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.OutputStream

class ZipBuilder(outputStream: OutputStream) {
  val zipOutputStream: ZipOutputStream = new ZipOutputStream(outputStream)

  def addZipEntry(fileName: String, document: File): Try[Boolean] = {
    val is = new FileInputStream(document)
    try {
      addZipEntry(fileName, is)
    } finally {
      is.close()
    }
  }

  def addZipEntry(fileName: String, document: Array[Byte]): Try[Boolean] = {
    Try {
      val zipEntry = new ZipEntry(fileName)
      zipOutputStream.putNextEntry(zipEntry)
      zipOutputStream.write(document)
      zipOutputStream.closeEntry()
      true
    }
  }

  def addZipEntry(fileName: String, is: InputStream): Try[Boolean] = {
    Try {
      val zipEntry = new ZipEntry(fileName)
      zipOutputStream.putNextEntry(zipEntry)
      val baos = new ByteArrayOutputStream()
      val bytes = new Array[Byte](1024);
      var length = is.read(bytes)
      while (length >= 0) {
        zipOutputStream.write(bytes, 0, length);
        length = is.read(bytes)
      }

      zipOutputStream.closeEntry()
      true
    }
  }

  def close(): Option[File] = {
    Try(zipOutputStream.close)
    None
  }
}