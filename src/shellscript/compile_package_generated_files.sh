#/bin/sh

source_folder=$1
class_path=$2
source_files=$3
jar_name=$4
target_project_dir=$5

cd $source_folder;
javac -verbose -cp $class_path -sourcepath $source_files $source_files;
echo "Compile finished!"

jar -cvf $jar_name com/*
echo "Jar created successfully!"

target_classpath_file=$target_project_dir"/.classpath"

jar_path=$source_folder"/"$jar_name

sed -i "/<\/classpath>/i \\\t<classpathentry kind=\"lib\" path=\"${jar_path}\"/>" $target_classpath_file
echo "Updated $target_classpath_file"