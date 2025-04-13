#!/bin/bash
#
# Assembles a custom runtime image with the required JavaFX modules
#
JAVA_HOME=/usr/lib64/jvm/java-21-openjdk-21
build_dir=../target
JAVAFX_HOME=$build_dir/javafx
jlink_path=$JAVA_HOME/bin/

module_path=$JAVAFX_HOME
modules=javafx.controls,javafx.fxml,java.base,java.logging,java.prefs
modules+=,java.net.http,java.naming,java.rmi,java.compiler

echo "Building runtime image..."
$jlink_path/jlink --verbose --strip-debug --no-header-files --no-man-pages\
 --module-path $module_path --add-modules $modules --output $build_dir/runtime/
