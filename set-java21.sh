#!/bin/bash
# Set Java 21 as default for this bot project

export JAVA_HOME=/Users/A86845395/Library/Java/JavaVirtualMachines/azul-21.0.6/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

echo "Java version set to:"
java -version

echo ""
echo "To make this permanent, add the following to your ~/.zshrc:"
echo "export JAVA_HOME=/Users/A86845395/Library/Java/JavaVirtualMachines/azul-21.0.6/Contents/Home"
echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\""