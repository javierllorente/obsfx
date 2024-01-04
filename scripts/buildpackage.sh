#!/bin/bash
#
# Builds a rpm package
#
name=obsfx
base_dir=..
license_type=Apache-2.0
license_file=$base_dir/LICENSE
menu_group="Development;Translation;"

build_dir=$base_dir/target
lib_dir=$build_dir/dist/lib
dist_dir=$build_dir/dist
jar_filepath=`ls $dist_dir/$name*.jar`
jar_filename=$(basename -- "$jar_filepath")
icon=$base_dir/src/main/resources/obs.png
version=${jar_filename:${#name}+1:-4}
release=0

JAVA_HOME=/usr/lib64/jvm/java-17-openjdk-17
JAVAFX_HOME=$build_dir/javafx
jpackage_path=$JAVA_HOME/bin/
module_path=:$JAVAFX_HOME:$lib_dir
modules=javafx.controls,javafx.fxml,java.base,java.logging,java.prefs,jdk.crypto.ec,java.naming
java_options='--add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED'

echo "Building package..."
$jpackage_path/jpackage --verbose --type rpm --app-version $version --linux-app-release $release \
--linux-rpm-license-type $license_type --license-file $license_file \
--input $dist_dir --main-jar $jar_filename --name $name --module-path $module_path --add-modules $modules \
--java-options "$java_options" --icon $icon --linux-shortcut --linux-menu-group $menu_group --dest $build_dir
