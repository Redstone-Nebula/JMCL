rootProject.name = "JVM-MCL3"
include(
    "JVM-MCL",
    "JVM-MCLCore",
    "JVM-MCLBoot"
)

// Map new module names to legacy directory names
project(":JVM-MCL").projectDir = file("HMCL")
project(":JVM-MCLCore").projectDir = file("HMCLCore")
project(":JVM-MCLBoot").projectDir = file("HMCLBoot")

val minecraftLibraries = listOf("JVM-MCLTransformerDiscoveryService", "JVM-MCLMultiMCBootstrap")
include(minecraftLibraries)

// Map new minecraft library names to legacy directories
project(":JVM-MCLTransformerDiscoveryService").projectDir = file("minecraft/libraries/HMCLTransformerDiscoveryService")
project(":JVM-MCLMultiMCBootstrap").projectDir = file("minecraft/libraries/HMCLMultiMCBootstrap")
