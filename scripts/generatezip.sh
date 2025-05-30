#!/bin/bash
#
# Generates a zip file with application, JavaFX and dependencies
#

if [ "$1" = "--jre" ] || [ "$1" == "--no-jre" ]; then
    echo "Preparing content..."
else
    echo "Available options:"
    echo "--jre"
    echo "--no-jre"
    exit 1
fi

name=obsfx
build_dir=../target
jar_filepath=`ls $build_dir/dist/$name*.jar`
jar_filename=$(basename -- "$jar_filepath")
version=${jar_filename:${#name}+1:-4}

zip_dir=$name
zip_name=$name-$version
launcher=$name.sh

lib_dir=lib
modules=javafx.controls,javafx.fxml,org.kordamp.ikonli.javafx,org.controlsfx.controls
modules+=,jakarta.inject,jakarta.annotation,org.apache.logging.log4j.slf4j2.impl
modules+=,com.javierllorente.jobs
exports=javafx.base/com.sun.javafx.event=org.controlsfx.controls

pushd $build_dir
mkdir $zip_dir

case $1 in
    "--jre")
        zip_name=$zip_name-jre.zip
        module_path=$lib_dir
        popd
        ./assembleruntime.sh
        pushd $build_dir
        runtime_dir=runtime
        java_bin=$runtime_dir/bin/
        cp -R $runtime_dir $zip_dir/
    ;;
    "--no-jre")
        zip_name=$zip_name-nojre.zip
        JAVAFX_HOME=javafx
        module_path=$JAVAFX_HOME:$lib_dir
        cp -R $JAVAFX_HOME $zip_dir/
    ;;
    *)
        echo "Unrecognised option"
        exit 1;
    ;;
esac

java_bin=${java_bin}java

cp -R dist/* $zip_dir/
cp ../LICENSE $zip_dir/
cp ../$name.png $zip_dir/

cat > $zip_dir/$launcher <<EOF
#!/bin/bash
#
# Script for launching OBS FX
#

$java_bin --module-path $module_path --add-modules $modules --add-exports $exports -jar $jar_filename
EOF
chmod +x $zip_dir/$launcher

echo "Generating zip for $name $version"
zip -r $zip_name $zip_dir

echo "Cleaning..."
rm -rf $zip_dir

if [ "$1" == "--jre" ]; then
    rm -rf $runtime_dir
fi
