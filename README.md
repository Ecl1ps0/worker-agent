Build and run agent

```
docker build -t worker-agent .

docker run --gpus all --rm --network jade-net --name worker-agent -e MAIN_HOST=main-agent worker-agent
```