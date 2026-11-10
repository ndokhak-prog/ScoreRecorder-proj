package com.example.scorerecorder

import java.io.File

/**
 * 테스트 산출물 디렉터리.
 *
 * 기본 경로는 `build/<name>` 이지만, 이전 실행에서 생성된 폴더가 다른 프로세스(이미지 뷰어/프리뷰)에
 * 의해 잠겨 쓰기가 불가능한 경우 `build/<name>-alt` 로 자동 대체한다.
 */
fun testOutputDir(name: String = "screens"): File {
    val primary = File("build/$name").apply { mkdirs() }
    if (primary.isDirectory && primary.canWrite()) return primary
    return File("build/$name-alt").apply { mkdirs() }
}
