package io.thernal.networkkit.network.api.data.client

import io.ktor.http.HttpMethod
import io.ktor.util.reflect.typeInfo

suspend inline fun <reified T> ApiClient.get(
    path: String,
    block: RequestBuilder.() -> Unit = {},
): T {
    val request = RequestBuilder(method = HttpMethod.Get, path = path).apply(block).build()
    return send(request = request, responseType = typeInfo<T>())
}

suspend inline fun <reified T> ApiClient.delete(
    path: String,
    block: RequestBuilder.() -> Unit = {},
): T {
    val request = RequestBuilder(method = HttpMethod.Delete, path = path).apply(block).build()
    return send(request = request, responseType = typeInfo<T>())
}

suspend inline fun <reified T> ApiClient.post(
    path: String,
    block: RequestBuilder.() -> Unit = {},
): T {
    val request = RequestBuilder(method = HttpMethod.Post, path = path).apply(block).build()
    return send(request = request, responseType = typeInfo<T>())
}

suspend inline fun <reified B : Any, reified T> ApiClient.post(
    path: String,
    body: B,
    block: RequestBuilder.() -> Unit = {},
): T {
    return sendWithBody(method = HttpMethod.Post, path = path, body = body, block = block)
}

suspend inline fun <reified B : Any, reified T> ApiClient.put(
    path: String,
    body: B,
    block: RequestBuilder.() -> Unit = {},
): T {
    return sendWithBody(method = HttpMethod.Put, path = path, body = body, block = block)
}

suspend inline fun <reified B : Any, reified T> ApiClient.patch(
    path: String,
    body: B,
    block: RequestBuilder.() -> Unit = {},
): T {
    return sendWithBody(method = HttpMethod.Patch, path = path, body = body, block = block)
}

@PublishedApi
internal suspend inline fun <reified B : Any, reified T> ApiClient.sendWithBody(
    method: HttpMethod,
    path: String,
    body: B,
    block: RequestBuilder.() -> Unit,
): T {
    val builder = RequestBuilder(method = method, path = path)
    builder.body(value = body, type = typeInfo<B>())
    val request = builder.apply(block).build()
    return send(request = request, responseType = typeInfo<T>())
}
