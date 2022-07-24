package jpabook.jpashop.service;

import jpabook.jpashop.domain.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    @Transactional // readOnly면 저장이 안됨
    public void saveItem(Item item) {
        itemRepository.save(item);
    }

    @Transactional
    public Item updateItem(Long itemId, String name, int price, int stockQuantity) { // 서비스 계층에서 영속 상태의 엔티티를 조회하고 엔티티의 데이터를 직접 변경한다.
        Item item = itemRepository.findOne(itemId);
        // set으로 넣는 것은 예제일 뿐. findItem.change(price, name, stockQuantity);와 같이 의미있는 메서드를 호출하는게 좋다.
        item.setPrice(price);
        item.setName(name);
        item.setStockQuantity(stockQuantity);
        return item;
    }

    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId) {
        return itemRepository.findOne(itemId);
    }
}
