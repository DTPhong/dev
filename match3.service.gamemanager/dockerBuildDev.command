docker build --platform linux/amd64 -t match3.service.gamemanager /Users/admin/Desktop/bliss/match3.service.gamemanager -f /Users/admin/Desktop/bliss/match3.service.gamemanager/conf/conf.dev/Dockerfile
docker tag match3.service.gamemanager asia-southeast1-docker.pkg.dev/match3-424804/dev/match3.service.gamemanager
docker push asia-southeast1-docker.pkg.dev/match3-424804/dev/match3.service.gamemanager
