int main() {
  int i;
  for (i = 0; i < 10; i = i + 1) {
    putchar(48 + i);
  }
  putchar(10);

  return 0;
}