package com.routeforge.coredomain

internal inline fun <T, E : DomainError, R> Result<T, E>.map(map: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }

internal inline fun <T, E : DomainError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> =
    when (this) {
        is Result.Error -> this
        is Result.Success -> {
            action(data)
            this
        }
    }

internal inline fun <T, E : DomainError> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> =
    when (this) {
        is Result.Error -> {
            action(error)
            this
        }
        is Result.Success -> this
    }

internal fun <T, E : DomainError> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }
