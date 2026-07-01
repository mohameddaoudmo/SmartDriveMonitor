#pragma once
#include <atomic>
#include <vector>
#include <cstddef>

template <typename T, size_t Capacity>
class SpscRingBuffer {
public:
    SpscRingBuffer() : head_(0), tail_(0) {
        // We allocate Capacity + 1 elements to distinguish between empty and full
        buffer_.resize(Capacity + 1);
    }

    // Producer writes to head
    bool enqueue(const T& item) {
        size_t head = head_.load(std::memory_order_relaxed);
        size_t tail = tail_.load(std::memory_order_acquire);
        
        size_t next_head = (head + 1) % buffer_.size();
        if (next_head == tail) {
            return false; // Buffer is full
        }
        
        buffer_[head] = item;
        head_.store(next_head, std::memory_order_release);
        return true;
    }

    // Consumer reads from tail
    bool dequeue(T& item) {
        size_t tail = tail_.load(std::memory_order_relaxed);
        size_t head = head_.load(std::memory_order_acquire);
        
        if (tail == head) {
            return false; // Buffer is empty
        }
        
        item = buffer_[tail];
        tail_.store((tail + 1) % buffer_.size(), std::memory_order_release);
        return true;
    }

    bool empty() const {
        return tail_.load(std::memory_order_relaxed) == head_.load(std::memory_order_relaxed);
    }

    size_t size() const {
        size_t head = head_.load(std::memory_order_relaxed);
        size_t tail = tail_.load(std::memory_order_relaxed);
        if (head >= tail) {
            return head - tail;
        } else {
            return buffer_.size() - tail + head;
        }
    }

private:
    std::vector<T> buffer_;
    std::atomic<size_t> head_;
    std::atomic<size_t> tail_;
};
