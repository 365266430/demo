import axios from "axios";

const request = axios.create({
    baseURL: "/api",
    timeout: 120000,
});

request.interceptors.response.use(
    (response) => {
        const res = response.data;

        if (res && res.code !== 200) {
            return Promise.reject(new Error(res.message || "请求失败"));
        }

        return res.data;
    },
    (error) => {
        const message =
            error?.response?.data?.message ||
            error?.message ||
            "网络请求失败";

        return Promise.reject(new Error(message));
    }
);

export default request;
