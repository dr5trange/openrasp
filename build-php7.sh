#!/bin/bash
#
# To enable native antlr support, do
# 
# wget https://packages.baidu.com/app/openrasp/libantlr4-linux.tar.gz -O /tmp/libantlr4.tar.gz
# tar -xf /tmp/libantlr4.tar.gz -C /tmp
# extra_config_opt="--with-antlr4=/tmp/libantlr4" bash build-php.sh
# 
# Chinese PHP extension compilation instructions
# https://rasp.baidu.com/doc/hacking/compile/php.html

cd "$(dirname "$0")"

set -ex
script_base="$(readlink -f $(dirname "$0"))"

# PHP version and architecture
php_version=$(php -r 'echo PHP_MAJOR_VERSION, ".", PHP_MINOR_VERSION;')
php_arch=$(uname -m)
php_os=

case "$(uname -s)" in
    Linux)     
		php_os=linux
		;;
    Darwin)
		php_os=macos
        ;;
    *)
		echo Unsupported OS: $(uname -s)
		exit 1
		;;
esac

# download libv8
curl https://packages.baidu.com/app/openrasp/libv8-5.9-"$php_os".tar.gz -o /tmp/libv8-5.9.tar.gz
tar -xf /tmp/libv8-5.9.tar.gz -C /tmp/

# Determine the build directory
output_base="$script_base/rasp-php-$(date +%Y-%m-%d)"
output_ext="$output_base/php/${php_os}-php${php_version}-${php_arch}"

#Compile 
cd agent/php7
phpize --clean
phpize
if [[ $php_os == "macos" ]]; then
	./configure --with-v8=/tmp/libv8-5.9-${php_os}/ --with-gettext=/usr/local/homebrew/opt/gettext -q ${extra_config_opt}
else
	./configure --with-v8=/tmp/libv8-5.9-${php_os}/ --with-gettext -q ${extra_config_opt}
fi

make

# replication extension
mkdir -p "$output_ext"
cp modules/openrasp.so "$output_ext"/
make distclean
phpize --clean

# Copy other files
mkdir -p "$output_base"/{conf,assets,logs,locale,plugins}
cp ../../plugins/official/plugin.js "$output_base"/plugins/official.js
cp ../../rasp-install/php/*.php "$output_base"

# Generate and copy mo files
./scripts/locale.sh
mv ./po/locale.tar "$output_base"/locale
cd "$output_base"/locale
tar xvf locale.tar && rm -f locale.tar

# Bale
cd "$script_base"
tar --numeric-owner --group=0 --owner=0 -cjvf "$script_base/rasp-php.tar.bz2" "$(basename "$output_base")"




