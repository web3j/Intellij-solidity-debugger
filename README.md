# Solidity debugger plugin for IntelliJ IDE

Adds debugging support for Solidity language and integrates Intellij with Web3j EVM.

## Features


## Installation

** This is still work in progress, and the plugin not published yet.

Building from the code

Build

```
./gradlew buildPlugin
```

Run 

```
./gradlew runIde
```

or

```
With Idea using gradle task, Tasks -> intellij -> runIde (in debugger mode)
```

After that, it should open a new Idea app with our debugging plugin.
Open your sample project with a solidity contract.

## Debugging (in a new window )

1) Make sure that Solidity plugin is installed.
2) Configure solidity preferences:
   Preferences -> Languages & Frameworks -> Solidity
    
    1. Use Solc to build project - true
    2. Standalone Solc path - "{your path}/bin/solc"
    3. Generate java stubs for contracts during compilation - true
    4. Style: Web3J
    
3) Configure Run/Debug Configurations
   1. Add new configuration Evm.
   2. Fill in the following lines: Name(for example "RunHelloWorld"), Contract Wrapper (for example "HelloWorld"), Solidity Contract (for example "HelloWorld").
   3. Fill in the field 'Deploy Methods' - "deploy"
   
4) Execute Run task('RunHelloWorld') in the debug mode.


## Compatible IDEs

The plugin is compatible with all IntelliJ-based IDEs starting from the version 2020.3

* [IntelliJ IDEA] Community Edition

** Requires
[Intellij Solidity] plugin to be installed.

## Web3j EVM

For standalone or embedded Ethereum node to run within Java process, head for [Web3J EVM] repo

[Intellij Solidity]: https://plugins.jetbrains.com/plugin/9475-intellij-solidity
[Web3J EVM]: https://github.com/web3j/web3j-evm 
[IntelliJ IDEA]: https://www.jetbrains.com/idea/
