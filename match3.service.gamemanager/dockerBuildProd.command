docker build --platform linux/amd64 -t match3.service.gamemanager /Users/admin/Desktop/bliss/match3.service.gamemanager --no-cache -f /Users/admin/Desktop/bliss/match3.service.gamemanager/conf/conf.prod/Dockerfile
docker tag match3.service.gamemanager asia-southeast1-docker.pkg.dev/match3-424804/match3-api/match3.service.gamemanager
docker push asia-southeast1-docker.pkg.dev/match3-424804/match3-api/match3.service.gamemanager
