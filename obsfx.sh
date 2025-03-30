#!/bin/bash
#
# Script for launching OBSFX
#
base_dir=target
jar_filepath=`ls $base_dir/dist/$name*.jar`
jar_filename=$(basename -- "$jar_filepath")
JAVAFX_HOME=$base_dir/javafx
lib_dir=$base_dir/dist/lib

module_path=$JAVAFX_HOME:$lib_dir
modules=javafx.controls,javafx.fxml,org.kordamp.ikonli.javafx,org.controlsfx.controls
modules+=,jakarta.inject,jakarta.annotation,org.apache.logging.log4j.slf4j2.impl
modules+=,com.javierllorente.jobs
exports=javafx.base/com.sun.javafx.event=org.controlsfx.controls

java --module-path $module_path --add-modules $modules --add-exports $exports -jar $jar_filepath
