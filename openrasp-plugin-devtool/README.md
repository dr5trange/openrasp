# OpenRASP

A CLI tool for [OpenRASP](https://rasp.baidu.com) JavaScript plugins development.

### Installation

Prerequisites: [Node.js](https://nodejs.org) (>=4.x) with npm version 3+

```bash
$ npm install -g openrasp
```

### Usage

Check the ability and syntax of the plugin:

```bash
$ rasp check

  Usage: rasp check <file>


  Options:

    -t, --test-dir <dir>  specify a custom test cases directory
    -h, --help            output usage information
```
Example:

```bash
$ rasp check plugin.js

  Plugin  Plugin code check code specification: 150ms
  Plugin  Plug-in code check simulation environment: 15ms
  Plugin  Plug-in ability test sql security DESC wp_users: 16ms
  Plugin  Plug-in ability test sql security select name, email from users where id = 1002: 0ms
  √ Plugin ability test sql is not safe  select name, email from users where id = 1002 and 1=2 union select table_name, table_schema from information_schema.tables: 0ms


  5 passing (238ms)
```
