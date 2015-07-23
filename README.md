

## Running a PBS Torque server with Docker

* `docker run -h pbs.local -p 10022:22 -rm -d --name torque --privileged agaveapi/torque bash bash-4.1# /usr/bin/supervisord &`

