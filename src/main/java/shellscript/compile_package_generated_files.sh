#/bin/sh

source_folder=$1
class_path=$2
source_files=$3
jar_name=$4
target_project_dir=$5

cd $source_folder;
javac -cp $class_path -sourcepath $source_files $source_files;
echo "Compile finished!"

jar -cf $jar_name com/* metaData.txt
echo "Jar created successfully!"

target_classpath_file=$target_project_dir"/.classpath"

jar_path=$source_folder"/"$jar_name
#echo $jar_path
exist_in_classpath=`grep $jar_name $target_classpath_file | wc -l`
#echo $exist_in_classpath
if [ $exist_in_classpath -eq 0 ]; then
	sed -i "/<\/classpath>/i \\\t<classpathentry kind=\"lib\" path=\"${jar_path}\"/>" $target_classpath_file
	echo "Updated $target_classpath_file"
fi

projectId=${target_project_dir##*/}
mvn install:install-file -DgroupId=com.renren.servicemonitor -DartifactId=$projectId -Dversion=1.0 -Dpackaging=jar -Dfile=$jar_path

target_pom_file=$target_project_dir"/pom.xml"
exist_in_pom=`grep com.renren.servicemonitor $target_pom_file | wc -l`

if [ $exist_in_pom -eq 0 ]; then
	sed -i "/<\/dependencies>/i <dependency>\n<groupId>com.renren.servicemonitor<\/groupId>\n<artifactId>$projectId<\/artifactId>\n<version>1.0<\/version>\n<\/dependency>\n" $target_pom_file
	echo "Update $target_pom_file"
fi
