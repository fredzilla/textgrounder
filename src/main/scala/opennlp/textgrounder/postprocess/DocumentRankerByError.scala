package opennlp.textgrounder.postprocess

// This program takes a log file and outputs the document names to standard out, ranked by prediction error.

import org.clapper.argot._
import opennlp.textgrounder.tr.topo._
import opennlp.textgrounder.tr.util.LogUtil

object DocumentRankerByError {

  import ArgotConverters._

  val parser = new ArgotParser("textgrounder run opennlp.textgrounder.postprocess.DocumentRankerByError", preUsage = Some("TextGrounder"))
  val logFile = parser.option[String](List("l", "log"), "log", "log input file")
  
  def main(args: Array[String]) {
    try {
      parser.parse(args)
    }
    catch {
      case e: ArgotUsageException => println(e.message); sys.exit(0)
    }

    if(logFile.value == None) {
      println("You must specify a log input file via -l.")
      sys.exit(0)
    }

    val docsAndErrors:List[(String, Double, Coordinate, Coordinate)] =
      (for(pe <- LogUtil.parseLogFile(logFile.value.get)) yield {
        val dist = pe.trueCoord.distanceInKm(pe.predCoord)

        (pe.docName, dist, pe.trueCoord, pe.predCoord)
      }).sortWith((x, y) => x._2 < y._2)

    for((docName, dist, trueCoord, predCoord) <- docsAndErrors) {
      println(docName+"\t"+dist+"\t"+trueCoord+"\t"+predCoord)
    }
  }
}