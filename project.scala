//> using scala 3.7.0
//> using dep "com.lihaoyi::os-lib:0.11.4"

object Bar{
    val foo = "Foo"
    val num = 15
}

@main def run(): Unit=
    val paths = os.list(os.pwd)
    println(os.pwd)
    println(paths.length)