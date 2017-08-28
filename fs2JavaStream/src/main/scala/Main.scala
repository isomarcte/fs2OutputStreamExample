import scala.language.higherKinds

import fs2._
import fs2.util._
import fs2.util.syntax._

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Random
import java.io.OutputStream
import java.io.FileOutputStream

object Main {

  def main(args: Array[String]): Unit =
    (for {
      tempFilePath <- this.tempFile[Task]("bigFile__", "__original")
      _ <- this.writeFile[Task](tempFilePath).run
      copyFilePath <- this.tempFile[Task]("bigFile__", "__copy")
      _ <- fs2.io.file.readAll[Task](tempFilePath, 1024 /* Chunk Size */).to(
        // Operates on a java.util.OutputStream
        fs2.io.writeOutputStream(this.outputStreamForCopy[Task](copyFilePath), true)
      ).run
      result <- Task.delay(
        println(s"File $tempFilePath copied to $copyFilePath using constant memory."))
    } yield result).unsafeRunSync

  // Setup //
  //
  // Create a file which is larger than heap. This part doesn't apply
  // to your use case, but is used to simulate it.
  //
  // If you are curious about something here and want me to expand on it, let
  // me know.

  def tempFile[E[_] : Effect](
    prefix: String,
    suffix: String
  ): E[Path] =
    Effect[E].delay(
      Files.createTempFile(
        prefix,
        suffix))

  def infiniteBytes[E[_] : Effect](
    rand: Random
  ): Stream[E, Byte] = {
    val runtime: Runtime = Runtime.getRuntime

    val buffer: Array[Byte] =
      new Array(scala.math.min(runtime.maxMemory / 2L, 1024L * 1024L * 10L).toInt)

    Stream.repeatEval(Effect[E].delay{rand.nextBytes(buffer); buffer}).flatMap(b => Stream.emits(b))
  }

  def sizedBytes[E[_] : Effect](
    rand: Random
  )(
    sizeInBytes: Long
  ): Stream[E, Byte] =
    this.infiniteBytes(rand).chunks.mapAccumulate(0L){
      case (acc, bytes) => (acc + bytes.size, bytes)
    }.takeWhile{
      case (acc, _) => acc < sizeInBytes
    }.flatMap{
      case (_, bytes) => Stream.chunk(bytes)
    }

  def writeFile[E[_] : Effect](
    path: Path
  ): Stream[E, Unit] =
    for {
      rand <- Stream.eval(Effect[E].delay(new Random()))
      _ <- this.sizedBytes(rand)(
        1024L * 1024L * 1024L * 3L // 3GiB
      ).to(fs2.io.file.writeAll(path, List(StandardOpenOption.WRITE)))
    } yield (())

  def outputStreamForCopy[E[_] : Effect](
    newFilePath: Path
  ): E[OutputStream] =
    Effect[E].delay(new FileOutputStream(newFilePath.toFile))
}
