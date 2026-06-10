rootProject.name = "JVM-MCL3"
include(
    "JVM-MCL",
    "JVM-MCLCore",
    "JVM-MCLBoot",
    "JVM-MCLCraft",
    "JVM-MCLPlay"
)

// Map new module names to legacy directory names
project(":JVM-MCL").projectDir = file("JMCL")
project(":JVM-MCLCore").projectDir = file("JMCLCore")
project(":JVM-MCLBoot").projectDir = file("JMCLBoot")
project(":JVM-MCLCraft").projectDir = file("Craft")
project(":JVM-MCLPlay").projectDir = file("play")

val minecraftLibraries = listOf("JVM-MCLTransformerDiscoveryService", "JVM-MCLMultiMCBootstrap")
include(minecraftLibraries)

// Map new minecraft library names to legacy directories
project(":JVM-MCLTransformerDiscoveryService").projectDir = file("minecraft/libraries/JMCLTransformerDiscoveryService")
project(":JVM-MCLMultiMCBootstrap").projectDir = file("minecraft/libraries/JMCLMultiMCBootstrap")